package de.hhu.bsinfo.dxraft.server;

import java.util.List;
import java.util.UUID;

import de.hhu.bsinfo.dxraft.client.message.*;
import de.hhu.bsinfo.dxraft.log.LogEntryFactory;
import de.hhu.bsinfo.dxraft.log.entry.ConfigChangeEntry;
import de.hhu.bsinfo.dxraft.net.datagram.DatagramRequestNetworkService;
import de.hhu.bsinfo.dxraft.net.dxnet.ServerDXNetNetworkService;
import de.hhu.bsinfo.dxraft.net.dxnet.message.DXNetServerMessageFactory;
import de.hhu.bsinfo.dxraft.server.message.*;
import de.hhu.bsinfo.dxraft.server.net.AbstractRequestNetworkService;
import de.hhu.bsinfo.dxraft.server.net.AbstractServerNetworkService;
import de.hhu.bsinfo.dxraft.server.net.RequestReceiver;
import de.hhu.bsinfo.dxraft.net.datagram.DatagramServerNetworkService;
import de.hhu.bsinfo.dxraft.server.net.ServerMessageReceiver;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.log.Log;
import de.hhu.bsinfo.dxraft.log.entry.LogEntry;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.timer.RaftTimer;
import de.hhu.bsinfo.dxraft.timer.TimeoutHandler;

@Setter
public final class RaftServer implements ServerMessageReceiver, RequestReceiver, TimeoutHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private AbstractServerNetworkService m_networkService;
    private AbstractRequestNetworkService m_requestNetworkService;
    private ServerMessageFactory m_serverMessageFactory;
    private ResponseMessageFactory m_responseMessageFactory;
    private LogEntryFactory m_logEntryFactory;
    private ServerConfig m_context;
    @Getter
    private ServerState m_state;
    private Log m_log;
    private RaftTimer m_timer;

    private boolean m_started = false;

    public RaftServer(ServerConfig p_context) {
        m_context = p_context;

        // set id for logging file if not already set
        System.setProperty("server.id", String.valueOf(p_context.getLocalId()));
        LOGGER.info("Constructing server with id {}", p_context.getLocalId());
    }

    private void init() {
        if (m_networkService == null) {
            if ("dxnet".equals(m_context.getServerMessagingService())) {
                m_networkService = new ServerDXNetNetworkService(m_context);
                m_serverMessageFactory = new DXNetServerMessageFactory();
            } else {
                m_networkService = new DatagramServerNetworkService(m_context);
                m_serverMessageFactory = new DefaultServerMessageFactory();
            }
        }

        if (m_requestNetworkService == null) {
            m_requestNetworkService = new DatagramRequestNetworkService(m_context);
        }

        m_networkService.setMessageReceiver(this);
        m_requestNetworkService.setRequestReceiver(this);

        if (m_serverMessageFactory == null) {
            m_serverMessageFactory = new DefaultServerMessageFactory();
        }

        if (m_responseMessageFactory == null) {
            m_responseMessageFactory = new DefaultResponseMessageFactory();
        }

        if (m_logEntryFactory == null) {
            m_logEntryFactory = new LogEntryFactory();
        }

        if (m_timer == null) {
            m_timer = new RaftTimer(m_context);
        }

        m_timer.setTimeoutHandler(this);

        if (m_state == null) {
            m_state = new ServerState(m_context, m_timer);
        }

        if (m_log == null) {
            m_log = new Log(m_context, m_state);
        }

        m_state.setLog(m_log);
    }

    public void bootstrapNewCluster() {
        if (!m_started) {
            init();

            // if server is started with empty context, instantly become leader
            // and add local address to log by processing a dummy request
            if (m_context.getServers().isEmpty()) {
                m_state.setState(ServerState.State.CANDIDATE);
                m_state.convertStateToLeader();
                LOGGER.info("Bootstrapping in standalone mode");

                DefaultRequest request = new DefaultRequest();
                request.setRequestType(Requests.CONFIG_CHANGE_REQUEST);
                request.setData(m_context.getRaftAddress());
                request.setId(UUID.randomUUID());
                processClientRequest(request);
            } else {
                LOGGER.info("Bootstrapping cluster with {} servers", m_context.getServerCount());
            }

            m_networkService.startReceiving();
            m_requestNetworkService.startReceiving();

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
            init();
            m_networkService.startReceiving();
            m_requestNetworkService.startReceiving();

            m_started = true;
        }
    }

    public void shutdown() {
        if (m_started) {
            m_networkService.stopReceiving();
            m_networkService.close();
            m_requestNetworkService.stopReceiving();
            m_requestNetworkService.close();
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
            response = m_serverMessageFactory.newVoteResponse(p_request.getSenderId(), m_state.getCurrentTerm(), true);
            m_state.updateVote(p_request.getSenderId());

        } else {
            response = m_serverMessageFactory.newVoteResponse(p_request.getSenderId(), m_state.getCurrentTerm(), false);
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
            AppendEntriesResponse response = m_serverMessageFactory.newAppendEntriesResponse(p_request.getSenderId(),
                m_state.getCurrentTerm(), false, -1);
            m_networkService.sendMessage(response);
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
        AppendEntriesResponse response = m_serverMessageFactory.newAppendEntriesResponse(p_request.getSenderId(), m_state.getCurrentTerm(),
            true, matchIndex);
        m_networkService.sendMessage(response);
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
    public synchronized void processClientRequest(Request p_request) {
        // serve request if server is leader
        if (m_state.isLeader()) {

/*            if (request.isReadRequest()) {
                LOGGER.debug("Received read request from client {}", request.getSenderAddress());

                ReadRequest readRequest = (ReadRequest) request;
                readRequest.onCommit(context, log.getStateMachine(), state);
                DataResponse response = readRequest.buildResponse();
                networkService.sendResponse(response);
            } else {*/

            LogEntry logEntry = m_logEntryFactory.getLogEntryFromRequest(p_request, m_state.getCurrentTerm());

            // check if request was already handled before
            if (m_log.contains(logEntry)) {

                LogEntry currentLogEntry = m_log.getEntryByIndex(m_log.indexOf(logEntry));
                currentLogEntry.updateClientAddress(p_request.getSenderAddress());

                // check if logEntry is already committed
                if (currentLogEntry.isCommitted()) {
                    // if request is committed, get the response and send it
                    RequestResponse response = currentLogEntry.buildResponse(m_responseMessageFactory);

                    if (response == null) {
                        // this should not happen
                        LOGGER.error("Received request from client {} with already existent id " +
                            "but response could not be built", p_request.getSenderAddress());
                    } else {
                        m_requestNetworkService.sendResponse(response);
                    }
                }
            } else {

                if (logEntry instanceof ConfigChangeEntry) {
                    m_state.addPendingConfigChangeRequest(logEntry);
                    if (m_state.configChangeRequestisPending()) {
                        LOGGER.debug("Configuration change already in progress -> " +
                            "adding request to pending configuration change requests");
                        return;
                    }
                }

                m_log.append(logEntry);

                LOGGER.debug("Received request from client {} -> Sending append entries requests to followers",
                    p_request.getSenderAddress());

                // instantly commit entry if no other server in cluster
                // else send append entries requests
                if (m_context.singleServerCluster()) {
                    commitEntries();
                } else {
                    for (short server : m_context.getOtherServerIds()) {
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
            m_requestNetworkService.sendResponse(m_responseMessageFactory.newRequestResponse(
                p_request.getSenderAddress(), p_request.getId(),
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
            if (m_context.singleServerCluster()) {
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
        for (short id : m_context.getOtherServerIds()) {
            AppendEntriesRequest request = m_serverMessageFactory.newAppendEntriesRequest(id,
                m_state.getCurrentTerm(), m_log.getLastIndex(), m_log.isEmpty() ? -1 : m_log.getLastTerm(),
                m_log.getCommitIndex(), null);
            m_networkService.sendMessage(request);
        }
    }

    private void sendAppendEntriesRequest(short p_followerId) {
        if (!m_state.isLeader()) {
            throw new IllegalStateException("Append entries request could not be sent because state is "
                + m_state.getState() + " but should be LEADER!");
        }

        int prevLogTerm = m_state.getNextIndex(p_followerId) == 0 ? -1 : m_log.getEntryByIndex(
            m_state.getNextIndex(p_followerId)-1).getTerm();

        AppendEntriesRequest request = m_serverMessageFactory.newAppendEntriesRequest(p_followerId, m_state.getCurrentTerm(),
                m_state.getNextIndex(p_followerId) - 1, prevLogTerm, m_log.getCommitIndex(),
                m_log.getNewestEntries(m_state.getNextIndex(p_followerId)));
        m_networkService.sendMessage(request);
    }

    /**
     * Sends vote requests to all servers.
     */
    private void sendVoteRequests() {
        for (short id : m_context.getOtherServerIds()) {
            VoteRequest voteRequest = m_serverMessageFactory.newVoteRequest(id,
                m_state.getCurrentTerm(), m_log.getLastIndex(),
                m_log.getLastIndex() >= 0 ? m_log.getLastEntry().getTerm() : -1);
            m_networkService.sendMessage(voteRequest);
        }
    }

    private void checkTerm(int p_term) {
        if (m_state.getCurrentTerm() < p_term) {
            m_state.updateTerm(p_term);
        }
    }

    /**
     * Commits all entries that were replicated on enough other servers. Also sends responses.
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
                if (entry instanceof ConfigChangeEntry) {
                    m_state.popPendingConfigChangeRequest();
                    LogEntry next = m_state.getPendingConfigChangeRequest();
                    if (next != null) {
                        m_log.append(next);

                        LOGGER.debug("Continuing with next pending configuration change");
                        // update logs of all servers
                        for (short server : m_context.getOtherServerIds()) {
                            sendAppendEntriesRequest(server);
                        }
                    }
                }

                RequestResponse dataResponse = entry.buildResponse(m_responseMessageFactory);

                if (dataResponse != null) {
                    LOGGER.debug("Sending response to client {} for index {} because it was committed",
                        dataResponse.getReceiverAddress(), m_log.indexOf(entry));
                    m_requestNetworkService.sendResponse(dataResponse);
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
        private AbstractRequestNetworkService m_requestNetworkService;
        private ServerMessageFactory m_serverMessageFactory;
        private ResponseMessageFactory m_responseMessageFactory;
        private LogEntryFactory m_logEntryFactory;
        private ServerConfig m_context;
        private ServerState m_state;
        private Log m_log;
        private RaftTimer m_timer;

        private RaftServerBuilder() {
        }

        public static RaftServerBuilder aRaftServer() {
            return new RaftServerBuilder();
        }

        public RaftServerBuilder withNetworkService(AbstractServerNetworkService m_networkService) {
            this.m_networkService = m_networkService;
            return this;
        }

        public RaftServerBuilder withRequestNetworkService(AbstractRequestNetworkService m_requestNetworkService) {
            this.m_requestNetworkService = m_requestNetworkService;
            return this;
        }

        public RaftServerBuilder withServerMessageFactory(ServerMessageFactory m_serverMessageFactory) {
            this.m_serverMessageFactory = m_serverMessageFactory;
            return this;
        }

        public RaftServerBuilder withResponseMessageFactory(ResponseMessageFactory m_responseMessageFactory) {
            this.m_responseMessageFactory = m_responseMessageFactory;
            return this;
        }

        public RaftServerBuilder withLogEntryFactory(LogEntryFactory m_logEntryFactory) {
            this.m_logEntryFactory = m_logEntryFactory;
            return this;
        }

        public RaftServerBuilder withContext(ServerConfig m_context) {
            this.m_context = m_context;
            return this;
        }

        public RaftServerBuilder withState(ServerState m_state) {
            this.m_state = m_state;
            return this;
        }

        public RaftServerBuilder withLog(Log m_log) {
            this.m_log = m_log;
            return this;
        }

        public RaftServerBuilder withTimer(RaftTimer m_timer) {
            this.m_timer = m_timer;
            return this;
        }

        public RaftServer build() {
            RaftServer raftServer = new RaftServer(m_context);
            raftServer.m_logEntryFactory = this.m_logEntryFactory;
            raftServer.m_requestNetworkService = this.m_requestNetworkService;
            raftServer.m_log = this.m_log;
            raftServer.m_state = this.m_state;
            raftServer.m_timer = this.m_timer;
            raftServer.m_networkService = this.m_networkService;
            raftServer.m_serverMessageFactory = this.m_serverMessageFactory;
            raftServer.m_responseMessageFactory = this.m_responseMessageFactory;
            return raftServer;
        }
    }
}
