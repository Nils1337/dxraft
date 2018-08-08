package de.hhu.bsinfo.dxraft.server;

import de.hhu.bsinfo.dxraft.client.RaftClient;
import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.log.*;
import de.hhu.bsinfo.dxraft.message.client.AddServerRequest;
import de.hhu.bsinfo.dxraft.message.client.ClientRequest;
import de.hhu.bsinfo.dxraft.message.client.ReadRequest;
import de.hhu.bsinfo.dxraft.message.client.RemoveServerRequest;
import de.hhu.bsinfo.dxraft.message.server.AppendEntriesRequest;
import de.hhu.bsinfo.dxraft.message.server.AppendEntriesResponse;
import de.hhu.bsinfo.dxraft.message.server.ClientRedirection;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.message.server.VoteRequest;
import de.hhu.bsinfo.dxraft.message.server.VoteResponse;
import de.hhu.bsinfo.dxraft.net.AbstractNetworkService;
import de.hhu.bsinfo.dxraft.state.HashMapState;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.net.DatagramNetworkService;
import de.hhu.bsinfo.dxraft.state.StateMachine;
import de.hhu.bsinfo.dxraft.timer.RaftTimer;
import de.hhu.bsinfo.dxraft.timer.TimeoutHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RaftServer implements ServerMessageReceiver, TimeoutHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private AbstractNetworkService networkService;
    private RaftServerContext context;
    private ServerState state;
    private Log log;
    private RaftTimer timer;

    private boolean started = false;

    private RaftServer(AbstractNetworkService networkService, RaftServerContext context, ServerState state, Log log, RaftTimer timer) {
        this.networkService = networkService;
        this.context = context;
        this.state = state;
        this.log = log;
        this.timer = timer;
        LOGGER.info("Constructing server with id {}", context.getLocalId());
    }

    public void bootstrapNewCluster() {
        if (!started) {
            networkService.setMessageReceiver(this);
            timer.setTimeoutHandler(this);

            // if server is started with empty context, instantly become leader and add local address to log by processing a dummy request
            if (context.getRaftServers().isEmpty()) {
                state.setState(ServerState.State.CANDIDATE);
                state.convertStateToLeader();
                LOGGER.info("Bootstrapping in standalone mode");
                processClientRequest(new AddServerRequest(context.getLocalAddress()));
            } else {
                LOGGER.info("Bootstrapping cluster with {} servers", context.getServerCount());
            }

            networkService.startReceiving();
            state.becomeActive();
            started = true;
        }
    }

    /**
     * Tries to join the existing cluster given in the context (sends an add server request)
     *
     * The context must contain the exact configuration given to the cluster at bootstrap, otherwise this server will believe the cluster is different than it really is.
     */
    public void joinExistingCluster() {
        if (!started) {
            LOGGER.info("Joining existing cluster...");
            networkService.setMessageReceiver(this);
            timer.setTimeoutHandler(this);

            networkService.startReceiving();
            started = true;
//
//            RaftContext clientContext = new RaftContext(context.getRaftServers(), new RaftAddress("127.0.0.1", 6000));
//
//            boolean success = new RaftClient(clientContext, networkService).addServer(context.getLocalAddress());
//            if (success) {
//                LOGGER.info("Successfully joined cluster");
//            } else {
//                shutdown();
//                LOGGER.error("Joining the cluster failed");
//            }
        }
    }

    public void shutdown() {
        if (started) {
            networkService.stopReceiving();
            networkService.close();
            state.becomeIdle();
        }
    }


    @Override
    public synchronized void processVoteRequest(VoteRequest request) {
        checkTerm(request.getTerm());

        VoteResponse response;
        if (state.isFollower() && request.getTerm() >= state.getCurrentTerm()
            && (state.getVotedFor() == null || state.getVotedFor().equals(request.getSenderId())) &&
                log.isAtLeastAsUpToDateAs(request.getLastLogTerm(), request.getLastLogIndex())) {
            response = new VoteResponse(request.getSenderId(), state.getCurrentTerm(), true);
            state.updateVote(request.getSenderId());

        } else {
            response = new VoteResponse(request.getSenderId(), state.getCurrentTerm(), false);
        }

        networkService.sendMessage(response);
    }

    @Override
    public synchronized void processVoteResponse(VoteResponse response) {
        checkTerm(response.getTerm());

        if (state.isCandidate() && response.getTerm() == state.getCurrentTerm()) {
            if (response.isVoteGranted()) {

                LOGGER.debug("Vote received from server {}", response.getSenderId());
                state.updateVote(response.getSenderId(), true);

                //check if server got quorum
                if (state.getVotesCount() > context.getServerCount()/2.0) {
                    //TODO append no-op to log and replicate so that entry with current term is in log
                    state.convertStateToLeader();
                    sendHeartbeat();
                }
            } else {
                LOGGER.debug("Vote rejection received from server {}", response.getSenderId());
                state.updateVote(response.getSenderId(), false);
            }
        }
    }

    @Override
    public synchronized void processAppendEntriesRequest(AppendEntriesRequest request) {
        checkTerm(request.getTerm(), request.getSenderId());
        if (state.getCurrentTerm() <= request.getTerm() && state.isFollower() && !state.isIdle()) {
            state.resetStateAsFollower();
        }

        // if server is candidate and receives an append entries request with its current term
        // it means a leader was already chosen and the server should convert back to follower
        if (state.getCurrentTerm() == request.getTerm()) {
            if (state.isCandidate()) {
                LOGGER.debug("Terminating election because received request from leader in current term ({})", state.getCurrentTerm());
                state.convertStateToFollower();;
            }

            state.updateLeader(request.getSenderId());
        }


        // return false if request term < local term or if logs differ
        if (state.getCurrentTerm() > request.getTerm() || log.isDiffering(request.getPrevLogIndex(), request.getPrevLogTerm())) {
            LOGGER.debug("Rejecting append entries request because it is an old request or the logs differ");
            networkService.sendMessage(new AppendEntriesResponse(request.getSenderId(), state.getCurrentTerm(),false));
            return;
        }

        int matchIndex;
        if (request.getEntries() != null && !request.getEntries().isEmpty()) {
            matchIndex = request.getPrevLogIndex() + request.getEntries().size();
            // update log
            log.updateEntries(request.getPrevLogIndex() + 1, request.getEntries());
            LOGGER.debug("Received an append entries request from server {} and updated log. The current matchIndex is {}", request.getSenderId(), matchIndex);
        } else {
            matchIndex = request.getPrevLogIndex();
            LOGGER.trace("Received a heartbeat from server {}. The current matchIndex is {}", request.getSenderId(), matchIndex);

        }

        // update commit index
        if (log.getCommitIndex() < request.getLeaderCommitIndex()) {
            int newCommitIndex = Math.min(request.getLeaderCommitIndex(), matchIndex);
            LOGGER.debug("Committing entries from indices {} to {} after getting new onCommit index from server {}", log.getCommitIndex() + 1, newCommitIndex, request.getSenderId());
            log.commitEntries(newCommitIndex);
        }

        //send response
        networkService.sendMessage(new AppendEntriesResponse(context.getLocalId(), request.getSenderId(), state.getCurrentTerm(), true, matchIndex));
    }

    @Override
    public synchronized void processAppendEntriesResponse(AppendEntriesResponse response) {
        checkTerm(response.getTerm());

        if (state.isLeader()) {
            // if append entries request was not successful and server is still leader, the state of the follower differs from the leader's state
            // -> decrease nextIndex of the follower and try again
            if (!response.isSuccess()) {

                LOGGER.debug("Received rejected append entries response from server {}, is trying again with decremented index",
                        response.getSenderId());
                state.decrementNextIndex(response.getSenderId());
                sendAppendEntriesRequest(response.getSenderId());
            }

            // if append entries request was successful, update the matchIndex and nextIndex for the follower
            if (response.isSuccess()) {

                LOGGER.trace("Received successful append entries response from server {} with matchIndex {}", response.getSenderId(), response.getMatchIndex());

                state.updateMatchIndex(response.getSenderId(), response.getMatchIndex());
                state.updateNextIndex(response.getSenderId(), response.getMatchIndex() + 1);

                int newCommitIndex = state.getNewCommitIndex();
                int currentCommitIndex = log.getCommitIndex();
                if (newCommitIndex > currentCommitIndex) {

                    LOGGER.info("Committing the log entries from indices {} to {} because they are replicated on a majority of servers", currentCommitIndex + 1, newCommitIndex);
                    List<LogEntry> committedEntries = log.commitEntries(newCommitIndex);

                    // send responses for every log entry that was handled by this server and is now committed
                    for (LogEntry entry: committedEntries) {

                        if (entry instanceof AddServerRequest || entry instanceof RemoveServerRequest) {
                            state.popPendingConfigChangeRequest();
                            ClientRequest next = state.getPendingConfigChangeRequest();
                            if (next != null) {
                                log.append(next);

                                LOGGER.debug("Continuing with next pending configuration change");
                                // update logs of all servers
                                for (RaftID server : context.getOtherServerIds()) {
                                    sendAppendEntriesRequest(server);
                                }
                            }
                        }

                        ClientResponse clientResponse = entry.buildResponse();

                        if (clientResponse != null) {
                            LOGGER.debug("Sending response to client {} for index {} because it was committed", clientResponse.getReceiverAddress(), log.indexOf(entry));

                            networkService.sendMessage(clientResponse);
                        } else {
                            LOGGER.trace("Wanted to send a response but response could not be built");
                        }
                    }
                }
            }
        }

    }


    // TODO bypass raft algorithm for read requests but make sure reads cannot be stale
    // TODO improve performance of configuration changes by catching up new servers before propagating the change to followers
    @Override
    public synchronized void processClientRequest(ClientRequest request) {

        // serve request if server is leader
        if (state.isLeader()) {

/*            if (request.isReadRequest()) {
                LOGGER.debug("Received read request from client {}", request.getSenderAddress());

                ReadRequest readRequest = (ReadRequest) request;
                readRequest.onCommit(context, log.getStateMachine(), state);
                ClientResponse response = readRequest.buildResponse();
                networkService.sendMessage(response);
            } else {*/

            // check if request was already handled before
            if (log.contains(request)) {

                LogEntry currentLogEntry = log.getEntryByIndex(log.indexOf(request));
                currentLogEntry.updateClientRequest(request);

                // check if logEntry is already committed
                if (currentLogEntry.isCommitted()) {
                    // if request is committed, get the response and send it
                    ClientResponse clientResponse = currentLogEntry.buildResponse();

                    if (clientResponse == null) {
                        // this should not happen
                        LOGGER.error("Received request from client {} with already existent id but response could not be built", request.getSenderAddress());
                    } else {
                        networkService.sendMessage(clientResponse);
                    }
                }
            } else {

                if (request.isConfigChangeRequest()) {
                    state.addPendingConfigChangeRequest(request);
                    if (state.configChangeRequestisPending()) {
                        LOGGER.debug("Configuration change already in progress -> adding request to pending configuration change requests");
                        return;
                    }
                }

                // set the term of the new log entry
                request.setTerm(state.getCurrentTerm());

                log.append(request);

                LOGGER.debug("Received request from client {} -> Sending append entries requests to followers", request.getSenderAddress());
                // update logs of all servers
                for (RaftID server : context.getOtherServerIds()) {
                    sendAppendEntriesRequest(server);
                }
            }
//            }

        // else redirect client to current leader
        } else {
            LOGGER.debug("Received request from client but not leader -> Redirecting client to server {}!", state.getCurrentLeader());
            RaftID currentLeader = state.getCurrentLeader();
            networkService.sendMessage(new ClientRedirection(request.getSenderAddress(), currentLeader == null ? null : context.getAddressById(currentLeader)));
        }


    }

    @Override
    public synchronized void processTimeout() {
        LOGGER.trace("Timeout as {}", state.getState());
        if (state.isFollower()) {

            // server timed out as follower, so the leader is not available
            // -> server becomes candidate and starts election
            state.convertStateToCandidate();
            sendVoteRequests();

        } else if (state.isCandidate()) {

            // server timed out as candidate, election was not successful
            // -> start new election
            state.resetStateAsCandidate();
            sendVoteRequests();

        } else {

            state.resetStateAsLeader();
            // server is leader, send heartbeat
            sendHeartbeat();

        }

    }

    /**
     * Sends heartbeats to all servers.
     */
    private void sendHeartbeat() {
        AppendEntriesRequest request = new AppendEntriesRequest(null, state.getCurrentTerm(), log.getLastIndex(), log.isEmpty() ? -1 : log.getLastTerm(), log.getCommitIndex(), null);
        networkService.sendMessageToAllServers(request);
    }

    private void sendAppendEntriesRequest(RaftID followerId) {
        if (!state.isLeader()) {
            throw new IllegalStateException("Append entries request could not be sent because state is " + state.getState() + " but should be LEADER!");
        }

        int prevLogTerm = state.getNextIndex(followerId) == 0 ? -1 : log.getEntryByIndex(state.getNextIndex(followerId)-1).getTerm();

        AppendEntriesRequest request = new AppendEntriesRequest(followerId, state.getCurrentTerm(),
                state.getNextIndex(followerId) - 1, prevLogTerm, log.getCommitIndex(), log.getNewestEntries(state.getNextIndex(followerId)));
        networkService.sendMessage(request);
    }

    /**
     * Sends vote requests to all servers.
     */
    private void sendVoteRequests() {
        VoteRequest voteRequest = new VoteRequest(null, state.getCurrentTerm(), log.getLastIndex(), log.getLastIndex() >= 0 ? log.getLastEntry().getTerm() : -1);
        networkService.sendMessageToAllServers(voteRequest);
    }

    private void checkTerm(int term) {
        if (state.getCurrentTerm() < term) {
            state.updateTerm(term);
        }
    }

    /**
     * Checks if the term of the message is higher than the local term. If this is the case, state is changed to follower. Also updates the leader to the provided leader id.
     * @param term
     * @param leader
     */
    public void checkTerm(int term, RaftID leader) {
        if (state.getCurrentTerm() < term) {
            state.updateTerm(term);
            state.updateLeader(leader);
        }
    }

    public static void main(String[] args) {

        int initServerCount = 0;
        int id = -1;
        boolean join = false;

        for (String arg: args) {
            if (arg.startsWith("-id=")) {
                id = Short.parseShort(arg.substring(4));
            } else if (arg.startsWith("-initServerCount=")) {
                initServerCount = Integer.parseInt(arg.substring(17));
            } else if (arg.equals("--join")) {
                join = true;
            }
        }

        if (id == -1) {
            System.out.println("Id must be provided!");
            System.exit(-1);
        }

        //for logging
        System.setProperty("serverId", "server-" + id);


        List<RaftAddress> servers = new ArrayList<>();
        for (int i = 0; i < initServerCount; i++) {
            servers.add(new RaftAddress(new RaftID(i), "127.0.0.1", 5000 + i));
        }

        RaftAddress localAddress = new RaftAddress(new RaftID(id), "127.0.0.1", 5000 + id);

        RaftServerContext context = RaftServerContext.RaftServerContextBuilder
                .aRaftServerContext()
                .withLocalAddress(localAddress)
                .withRaftServers(servers)
                .build();

        RaftServer server = RaftServerBuilder
                .aRaftServer()
                .withContext(context)
                .build();

        if (!join) {
            server.bootstrapNewCluster();
        } else {
            server.joinExistingCluster();
        }
    }

    public static final class RaftServerBuilder {
        private AbstractNetworkService networkService;
        private RaftServerContext context;
        private ServerState state;
        private Log log;
        private LogStorage logStorage;
        private StateMachine stateMachine;
        private RaftTimer timer;

        private RaftServerBuilder() {
        }

        public static RaftServerBuilder aRaftServer() {
            return new RaftServerBuilder();
        }

        public RaftServerBuilder withNetworkService(AbstractNetworkService networkService) {
            this.networkService = networkService;
            return this;
        }

        public RaftServerBuilder withContext(RaftServerContext context) {
            this.context = context;
            return this;
        }

        public RaftServerBuilder withState(ServerState state) {
            this.state = state;
            return this;
        }

        public RaftServerBuilder withLog(Log log) {
            this.log = log;
            return this;
        }

        public RaftServerBuilder withLogStorage(LogStorage logStorage) {
            this.logStorage = logStorage;
            return this;
        }

        public RaftServerBuilder withStateMachine(StateMachine stateMachine) {
            this.stateMachine = stateMachine;
            return this;
        }

        public RaftServerBuilder withTimer(RaftTimer timer) {
            this.timer = timer;
            return this;
        }

        public RaftServer build() {
            if (context == null) {
                throw new IllegalArgumentException("Context must be provided!");
            }

            if (stateMachine == null) {
                stateMachine = new HashMapState();
            }

            if (logStorage == null) {
                logStorage = new InMemoryLog(context);
            }

            if (log == null) {
                log = new Log(context);
            }

            if (networkService == null) {
                networkService = new DatagramNetworkService(context, false);
            }

            if (timer == null) {
                timer = new RaftTimer(context);
            }

            if (state == null) {
                state = new ServerState(context);
            }

            logStorage.setState(state);
            logStorage.setStateMachine(stateMachine);

            log.setLogStorage(logStorage);
            log.setStateMachine(stateMachine);
            log.setState(state);

            state.setLog(log);
            state.setTimer(timer);

            return new RaftServer(networkService, context, state, log, timer);
        }
    }
}
