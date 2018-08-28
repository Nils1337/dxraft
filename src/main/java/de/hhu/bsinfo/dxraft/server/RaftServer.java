package de.hhu.bsinfo.dxraft.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hhu.bsinfo.dxraft.net.RaftAddress;
import de.hhu.bsinfo.dxraft.log.InMemoryLog;
import de.hhu.bsinfo.dxraft.log.Log;
import de.hhu.bsinfo.dxraft.log.LogEntry;
import de.hhu.bsinfo.dxraft.log.LogStorage;
import de.hhu.bsinfo.dxraft.message.RaftMessage;
import de.hhu.bsinfo.dxraft.message.client.AddServerRequest;
import de.hhu.bsinfo.dxraft.message.client.AbstractClientRequest;
import de.hhu.bsinfo.dxraft.message.client.RemoveServerRequest;
import de.hhu.bsinfo.dxraft.message.server.AppendEntriesRequest;
import de.hhu.bsinfo.dxraft.message.server.AppendEntriesResponse;
import de.hhu.bsinfo.dxraft.message.server.ClientRedirection;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.message.server.VoteRequest;
import de.hhu.bsinfo.dxraft.message.server.VoteResponse;
import de.hhu.bsinfo.dxraft.net.ServerDatagramNetworkService;
import de.hhu.bsinfo.dxraft.net.AbstractServerNetworkService;
import de.hhu.bsinfo.dxraft.state.HashMapState;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;
import de.hhu.bsinfo.dxraft.timer.RaftTimer;
import de.hhu.bsinfo.dxraft.timer.TimeoutHandler;

public final class RaftServer implements ServerMessageReceiver, TimeoutHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private AbstractServerNetworkService m_networkService;
    private ServerContext m_context;
    private ServerState m_state;
    private Log m_log;
    private RaftTimer m_timer;

    private boolean m_started = false;

    private RaftServer(AbstractServerNetworkService p_networkService, ServerContext p_context, ServerState p_state,
        Log p_log, RaftTimer p_timer) {
        m_networkService = p_networkService;
        m_context = p_context;
        m_state = p_state;
        m_log = p_log;
        m_timer = p_timer;
        // set id for logging file if not already set
        System.setProperty("server.id", String.valueOf(p_context.getLocalId()));
        LOGGER.info("Constructing server with id {}", p_context.getLocalId());
    }

    public void bootstrapNewCluster() {
        if (!m_started) {
            m_networkService.setMessageReceiver(this);
            m_timer.setTimeoutHandler(this);

            // if server is started with empty context, instantly become leader
            // and add local address to log by processing a dummy request
            if (m_context.getServers().isEmpty()) {
                m_state.setState(ServerState.State.CANDIDATE);
                m_state.convertStateToLeader();
                LOGGER.info("Bootstrapping in standalone mode");
                processClientRequest(new AddServerRequest(m_context.getLocalAddress()));
            } else {
                LOGGER.info("Bootstrapping cluster with {} servers", m_context.getServerCount());
            }

            m_networkService.startReceiving();
            m_state.becomeActive();
            m_started = true;
        }
    }

    /**
     * Tries to join the existing cluster given in the context (sends an add server request)
     * The context must contain the exact configuration given to the cluster at bootstrap,
     * otherwise this server will believe the cluster is different than it really is.
     */
    public void joinExistingCluster() {
        if (!m_started) {
            LOGGER.info("Joining existing cluster...");
            m_networkService.setMessageReceiver(this);
            m_timer.setTimeoutHandler(this);
            m_networkService.startReceiving();
            m_started = true;
        }
    }

    public void shutdown() {
        if (m_started) {
            m_networkService.stopReceiving();
            m_networkService.close();
            m_state.becomeIdle();
        }
    }


    @Override
    public synchronized void processVoteRequest(VoteRequest p_request) {
        checkTerm(p_request.getTerm());

        VoteResponse response;
        if (m_state.isFollower() && p_request.getTerm() >= m_state.getCurrentTerm()
            && (m_state.getVotedFor() == RaftAddress.INVALID_ID || m_state.getVotedFor() == p_request.getSenderId()) &&
                m_log.isAtLeastAsUpToDateAs(p_request.getLastLogTerm(), p_request.getLastLogIndex())) {
            response = new VoteResponse(p_request.getSenderId(), m_state.getCurrentTerm(), true);
            m_state.updateVote(p_request.getSenderId());

        } else {
            response = new VoteResponse(p_request.getSenderId(), m_state.getCurrentTerm(), false);
        }

        m_networkService.sendMessage(response);
    }

    @Override
    public synchronized void processVoteResponse(VoteResponse p_response) {
        checkTerm(p_response.getTerm());

        if (m_state.isCandidate() && p_response.getTerm() == m_state.getCurrentTerm()) {
            if (p_response.isVoteGranted()) {

                LOGGER.debug("Vote received from server {}", p_response.getSenderId());
                m_state.updateVote(p_response.getSenderId(), true);

                //check if server got quorum
                if (m_state.getVotesCount() > m_context.getServerCount()/2.0) {
                    //TODO append no-op to log and replicate so that entry with current term is in log (needed to prevent stale reads)
                    m_state.convertStateToLeader();
                    sendHeartbeat();
                }
            } else {
                LOGGER.debug("Vote rejection received from server {}", p_response.getSenderId());
                m_state.updateVote(p_response.getSenderId(), false);
            }
        }
    }

    @Override
    public synchronized void processAppendEntriesRequest(AppendEntriesRequest p_request) {
        checkTerm(p_request.getTerm(), p_request.getSenderId());
        if (m_state.getCurrentTerm() <= p_request.getTerm() && m_state.isFollower() && !m_state.isIdle()) {
            m_state.resetStateAsFollower();
        }

        // if server is candidate and receives an append entries request with its current term
        // it means a leader was already chosen and the server should convert back to follower
        if (m_state.getCurrentTerm() == p_request.getTerm()) {
            if (m_state.isCandidate()) {
                LOGGER.debug("Terminating election because received request from leader in current term ({})",
                    m_state.getCurrentTerm());
                m_state.convertStateToFollower();;
            }

            m_state.updateLeader(p_request.getSenderId());
        }


        // return false if request term < local term or if logs differ
        if (m_state.getCurrentTerm() > p_request.getTerm() || m_log.isDiffering(p_request.getPrevLogIndex(),
            p_request.getPrevLogTerm())) {
            LOGGER.debug("Rejecting append entries request because it is an old request or the logs differ");
            m_networkService.sendMessage(new AppendEntriesResponse(p_request.getSenderId(), m_state.getCurrentTerm(),false));
            return;
        }

        int matchIndex;
        if (p_request.getEntries() != null && !p_request.getEntries().isEmpty()) {
            matchIndex = p_request.getPrevLogIndex() + p_request.getEntries().size();
            // update log
            m_log.updateEntries(p_request.getPrevLogIndex() + 1, p_request.getEntries());
            LOGGER.debug("Received an append entries request from server {} and updated log. " +
                "The current matchIndex is {}", p_request.getSenderId(), matchIndex);
        } else {
            matchIndex = p_request.getPrevLogIndex();
            LOGGER.trace("Received a heartbeat from server {}. The current matchIndex is {}", p_request.getSenderId(),
                matchIndex);

        }

        // update commit index
        if (m_log.getCommitIndex() < p_request.getLeaderCommitIndex()) {
            int newCommitIndex = Math.min(p_request.getLeaderCommitIndex(), matchIndex);
            LOGGER.debug("Committing entries from indices {} to {} after getting new onCommit index from server {}",
                m_log.getCommitIndex() + 1, newCommitIndex, p_request.getSenderId());
            m_log.commitEntries(newCommitIndex);
        }

        //send response
        m_networkService.sendMessage(new AppendEntriesResponse(p_request.getSenderId(), m_state.getCurrentTerm(),
            true, matchIndex));
    }

    @Override
    public synchronized void processAppendEntriesResponse(AppendEntriesResponse p_response) {
        checkTerm(p_response.getTerm());

        if (m_state.isLeader()) {
            // if append entries request was not successful and server is still leader,
            // the state of the follower differs from the leader's state
            // -> decrease nextIndex of the follower and try again
            if (!p_response.isSuccess()) {

                LOGGER.debug("Received rejected append entries response from server {}, " +
                        "is trying again with decremented index",
                        p_response.getSenderId());
                m_state.decrementNextIndex(p_response.getSenderId());
                sendAppendEntriesRequest(p_response.getSenderId());
            }

            // if append entries request was successful, update the matchIndex and nextIndex for the follower
            if (p_response.isSuccess()) {

                LOGGER.trace("Received successful append entries response from server {} with matchIndex {}",
                    p_response.getSenderId(), p_response.getMatchIndex());

                m_state.updateMatchIndex(p_response.getSenderId(), p_response.getMatchIndex());
                m_state.updateNextIndex(p_response.getSenderId(), p_response.getMatchIndex() + 1);

                commitEntries();
            }
        }

    }


    // TODO bypass raft algorithm for read requests but make sure reads cannot be stale
    // TODO improve performance of configuration changes by catching up new servers before propagating the change to followers
    @Override
    public synchronized void processClientRequest(AbstractClientRequest request) {
        // serve request if server is leader
        if (m_state.isLeader()) {

/*            if (request.isReadRequest()) {
                LOGGER.debug("Received read request from client {}", request.getSenderAddress());

                ReadRequest readRequest = (ReadRequest) request;
                readRequest.onCommit(context, log.getStateMachine(), state);
                ClientResponse response = readRequest.buildResponse();
                networkService.sendMessage(response);
            } else {*/

            // check if request was already handled before
            if (m_log.contains(request)) {

                LogEntry currentLogEntry = m_log.getEntryByIndex(m_log.indexOf(request));
                currentLogEntry.updateClientRequest(request);

                // check if logEntry is already committed
                if (currentLogEntry.isCommitted()) {
                    // if request is committed, get the response and send it
                    ClientResponse clientResponse = currentLogEntry.buildResponse();

                    if (clientResponse == null) {
                        // this should not happen
                        LOGGER.error("Received request from client {} with already existent id " +
                            "but response could not be built", request.getSenderAddress());
                    } else {
                        m_networkService.sendMessage(clientResponse);
                    }
                }
            } else {

                if (request.isConfigChangeRequest()) {
                    m_state.addPendingConfigChangeRequest(request);
                    if (m_state.configChangeRequestisPending()) {
                        LOGGER.debug("Configuration change already in progress -> " +
                            "adding request to pending configuration change requests");
                        return;
                    }
                }

                // set the term of the new log entry
                request.setTerm(m_state.getCurrentTerm());

                m_log.append(request);

                LOGGER.debug("Received request from client {} -> Sending append entries requests to followers",
                    request.getSenderAddress());

                // instantly commit entry if no other server in cluster
                // else send append entries requests
                if (m_context.singleServerCluster()) {
                    commitEntries();
                } else {
                    for (int server : m_context.getOtherServerIds()) {
                        sendAppendEntriesRequest(server);
                    }
                }
            }
//            }

        // else redirect client to current leader
        } else {
            LOGGER.debug("Received request from client but not leader -> Redirecting client to server {}!",
                m_state.getCurrentLeader());
            int currentLeader = m_state.getCurrentLeader();
            m_networkService.sendMessage(new ClientRedirection(request.getSenderAddress(),
                currentLeader == RaftAddress.INVALID_ID ? null : m_context.getAddressById(currentLeader)));
        }


    }

    @Override
    public synchronized void processTimeout() {
        LOGGER.trace("Timeout as {}", m_state.getState());
        if (m_state.isFollower()) {

            // server timed out as follower, so the leader is not available
            // -> server becomes candidate and starts election
            m_state.convertStateToCandidate();

            // instantly become leader if no other servers in cluster
            // else send vote requests
            if (m_context.getOtherRaftServers().isEmpty()) {
                m_state.convertStateToLeader();
            } else {
                sendVoteRequests();
            }


        } else if (m_state.isCandidate()) {

            // server timed out as candidate, election was not successful
            // -> start new election
            m_state.resetStateAsCandidate();
            sendVoteRequests();

        } else {

            m_state.resetStateAsLeader();
            // server is leader, send heartbeat
            sendHeartbeat();

        }

    }

    /**
     * Sends heartbeats to all servers.
     */
    private void sendHeartbeat() {
        AppendEntriesRequest request = new AppendEntriesRequest(RaftAddress.INVALID_ID,
            m_state.getCurrentTerm(), m_log.getLastIndex(), m_log.isEmpty() ? -1 : m_log.getLastTerm(),
            m_log.getCommitIndex(), null);
        sendMessageToAllServers(request);
    }

    private void sendAppendEntriesRequest(int p_followerId) {
        if (!m_state.isLeader()) {
            throw new IllegalStateException("Append entries request could not be sent because state is "
                + m_state.getState() + " but should be LEADER!");
        }

        int prevLogTerm = m_state.getNextIndex(p_followerId) == 0 ? -1 : m_log.getEntryByIndex(
            m_state.getNextIndex(p_followerId)-1).getTerm();

        AppendEntriesRequest request = new AppendEntriesRequest(p_followerId, m_state.getCurrentTerm(),
                m_state.getNextIndex(p_followerId) - 1, prevLogTerm, m_log.getCommitIndex(),
                m_log.getNewestEntries(m_state.getNextIndex(p_followerId)));
        m_networkService.sendMessage(request);
    }

    /**
     * Sends vote requests to all servers.
     */
    private void sendVoteRequests() {
        VoteRequest voteRequest = new VoteRequest(RaftAddress.INVALID_ID,
            m_state.getCurrentTerm(), m_log.getLastIndex(),
            m_log.getLastIndex() >= 0 ? m_log.getLastEntry().getTerm() : -1);
        sendMessageToAllServers(voteRequest);
    }

    private void sendMessageToAllServers(RaftMessage p_message) {
        for (int id : m_context.getOtherServerIds()) {
            p_message.setReceiverId(id);
            m_networkService.sendMessage(p_message);
        }
    }

    private void checkTerm(int p_term) {
        if (m_state.getCurrentTerm() < p_term) {
            m_state.updateTerm(p_term);
        }
    }

    /**
     * Commits all entries that were replicated on enough other servers. Sends
     */
    private void commitEntries() {
        int newCommitIndex = m_state.getNewCommitIndex();
        int currentCommitIndex = m_log.getCommitIndex();
        if (newCommitIndex > currentCommitIndex) {
            LOGGER.info("Committing the log entries from indices {} to {} " +
                "because they are replicated on a majority of servers", currentCommitIndex + 1, newCommitIndex);
            List<LogEntry> committedEntries = m_log.commitEntries(newCommitIndex);

            // send responses for every log entry that was handled by this server and is now committed
            for (LogEntry entry: committedEntries) {

                // if the committed entry was a configuration change
                // we can now handle the next configuration change request
                if (entry instanceof AddServerRequest || entry instanceof RemoveServerRequest) {
                    m_state.popPendingConfigChangeRequest();
                    AbstractClientRequest next = m_state.getPendingConfigChangeRequest();
                    if (next != null) {
                        m_log.append(next);

                        LOGGER.debug("Continuing with next pending configuration change");
                        // update logs of all servers
                        for (int server : m_context.getOtherServerIds()) {
                            sendAppendEntriesRequest(server);
                        }
                    }
                }

                ClientResponse clientResponse = entry.buildResponse();

                if (clientResponse != null) {
                    LOGGER.debug("Sending response to client {} for index {} because it was committed",
                        clientResponse.getReceiverAddress(), m_log.indexOf(entry));
                    m_networkService.sendMessage(clientResponse);
                } else {
                    LOGGER.trace("Wanted to send a response but response could not be built");
                }
            }
        }
    }

    /**
     * Checks if the term of the message is higher than the local term.
     * If this is the case, state is changed to follower.
     * Also updates the leader to the provided leader id.
     * @param p_term term of message
     * @param p_leader id of new leader
     */
    public void checkTerm(int p_term, int p_leader) {
        if (m_state.getCurrentTerm() < p_term) {
            m_state.updateTerm(p_term);
            m_state.updateLeader(p_leader);
        }
    }

    public static final class RaftServerBuilder {
        private AbstractServerNetworkService m_networkService;
        private ServerContext m_context;
        private ServerState m_state;
        private Log m_log;
        private LogStorage m_logStorage;
        private StateMachine m_stateMachine;
        private RaftTimer m_timer;

        private RaftAddress m_localAddress;
        private List<RaftAddress> m_raftServers = new ArrayList<>();
        private int m_followerTimeoutDuration = 100;
        private int m_followerRandomizationAmount = 50;
        private int m_electionTimeoutDuration = 100;
        private int m_electionRandomizationAmount = 50;
        private int m_heartbeatTimeoutDuration = 50;
        private int m_heartbeatRandomizationAmount = 0;

        private RaftServerBuilder() {
        }

        public static RaftServerBuilder aRaftServer() {
            return new RaftServerBuilder();
        }

        public RaftServerBuilder withRaftServers(List<RaftAddress> p_raftServers) {
            m_raftServers = p_raftServers;
            return this;
        }

        public RaftServerBuilder withFollowerTimeoutDuration(int p_followerTimeoutDuration) {
            m_followerTimeoutDuration = p_followerTimeoutDuration;
            return this;
        }

        public RaftServerBuilder withFollowerRandomizationAmount(int p_followerRandomizationAmount) {
            m_followerRandomizationAmount = p_followerRandomizationAmount;
            return this;
        }

        public RaftServerBuilder withLocalAddress(RaftAddress p_localAddress) {
            m_localAddress = p_localAddress;
            return this;
        }

        public RaftServerBuilder withElectionTimeoutDuration(int p_electionTimeoutDuration) {
            m_electionTimeoutDuration = p_electionTimeoutDuration;
            return this;
        }

        public RaftServerBuilder withElectionRandomizationAmount(int p_electionRandomizationAmount) {
            m_electionRandomizationAmount = p_electionRandomizationAmount;
            return this;
        }

        public RaftServerBuilder withHeartbeatTimeoutDuration(int p_heartbeatTimeoutDuration) {
            m_heartbeatTimeoutDuration = p_heartbeatTimeoutDuration;
            return this;
        }

        public RaftServerBuilder withHeartbeatRandomizationAmount(int p_heartbeatRandomizationAmount) {
            m_heartbeatRandomizationAmount = p_heartbeatRandomizationAmount;
            return this;
        }

        public RaftServerBuilder withNetworkService(AbstractServerNetworkService p_networkService) {
            m_networkService = p_networkService;
            return this;
        }

        public RaftServerBuilder withContext(ServerContext p_context) {
            m_context = p_context;
            return this;
        }

        public RaftServerBuilder withState(ServerState p_state) {
            m_state = p_state;
            return this;
        }

        public RaftServerBuilder withLog(Log p_log) {
            m_log = p_log;
            return this;
        }

        public RaftServerBuilder withLogStorage(LogStorage p_logStorage) {
            m_logStorage = p_logStorage;
            return this;
        }

        public RaftServerBuilder withStateMachine(StateMachine p_stateMachine) {
            m_stateMachine = p_stateMachine;
            return this;
        }

        public RaftServerBuilder withTimer(RaftTimer p_timer) {
            m_timer = p_timer;
            return this;
        }

        public RaftServer build() {
            if (m_context == null) {
                if (m_localAddress == null) {
                    throw new IllegalArgumentException("Context with local address or " +
                        "only local address must be provided!");
                }

                m_context = new ServerContext(m_raftServers, m_localAddress,
                    m_followerTimeoutDuration, m_followerRandomizationAmount,
                    m_electionTimeoutDuration, m_electionRandomizationAmount,
                    m_heartbeatTimeoutDuration, m_heartbeatRandomizationAmount);
            }

            if (m_stateMachine == null) {
                m_stateMachine = new HashMapState();
            }

            if (m_logStorage == null) {
                m_logStorage = new InMemoryLog(m_context);
            }

            if (m_log == null) {
                m_log = new Log(m_context);
            }

            if (m_networkService == null) {
                m_networkService = new ServerDatagramNetworkService(m_context);
            }

            if (m_timer == null) {
                m_timer = new RaftTimer(m_context);
            }

            if (m_state == null) {
                m_state = new ServerState(m_context);
            }

            m_logStorage.setState(m_state);
            m_logStorage.setStateMachine(m_stateMachine);

            m_log.setLogStorage(m_logStorage);
            m_log.setStateMachine(m_stateMachine);
            m_log.setState(m_state);

            m_state.setLog(m_log);
            m_state.setTimer(m_timer);

            return new RaftServer(m_networkService, m_context, m_state, m_log, m_timer);
        }
    }
}
