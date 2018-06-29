package de.hhu.bsinfo.dxraft.server;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.state.Log;
import de.hhu.bsinfo.dxraft.message.*;
import de.hhu.bsinfo.dxraft.state.LogEntry;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.test.DatagramNetworkService;
import de.hhu.bsinfo.dxraft.timer.RaftTimer;
import de.hhu.bsinfo.dxraft.timer.TimeoutHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RaftServer implements ServerMessageReceiver, TimeoutHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private ServerNetworkService networkService;
    private RaftServerContext context;
    private ServerState state;
    private Log log;
    private RaftTimer timer;

    private boolean started = false;

    public RaftServer(ServerNetworkService networkService, RaftServerContext context, ServerState state, Log log, RaftTimer timer) {
        this.networkService = networkService;
        this.context = context;
        this.state = state;
        this.log = log;
        this.timer = timer;
    }


    public void start() {
        if (!started) {
            networkService.setMessageReceiver(this);
            timer.setTimeoutHandler(this);

            state.startTimer();
            this.networkService.start();
        }
    }


    @Override
    public synchronized void processVoteRequest(VoteRequest request) {
        checkTerm(request.getTerm());

        VoteResponse response;
        if (state.isFollower() && request.getTerm() >= state.getCurrentTerm() && (state.getVotedFor() == null || state.getVotedFor().equals(request.getSenderId())) &&
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

        if (state.isCandidate()) {
            if (response.isVoteGranted()) {

                LOGGER.debug("Server {} got vote from server {}!", context.getLocalId(), response.getSenderId());
                state.updateVote(response.getSenderId(), true);

                //check if server got quorum
                if (state.getVotesCount() >= Math.ceil(context.getServerCount()/2.0)) {
                    state.convertStateToLeader();
                    sendHeartbeat();
                }
            } else {

                LOGGER.debug("Server {} got rejection from server {}!", context.getLocalId(), response.getSenderId());
                state.updateVote(response.getSenderId(), false);
            }
        }
    }

    @Override
    public synchronized void processAppendEntriesRequest(AppendEntriesRequest request) {
        checkTerm(request.getTerm(), request.getSenderId());
        if (state.getCurrentTerm() <= request.getTerm()) {
            if (state.isFollower()) {
                state.resetStateAsFollower();
            }
        }

        // if server is candidate and receives an append entries request with its current term
        // it means a leader was already chosen and the server should convert back to follower
        if (state.getCurrentTerm() == request.getTerm()) {
            if (state.isCandidate()) {
                LOGGER.debug("Server {} terminating election because it got request from leader in current term {}!", context.getLocalId(), state.getCurrentTerm());
                state.convertStateToFollower();;
            }

            state.updateLeader(request.getSenderId());
        }


        // return false if request term < local term or if logs differ
        if (state.getCurrentTerm() > request.getTerm() || log.isDiffering(request.getPrevLogIndex(), request.getPrevLogTerm())) {
            LOGGER.debug("Server {} is rejecting append entries request because it is an old request or the logs differ!", context.getLocalId());
            networkService.sendMessage(new AppendEntriesResponse(request.getSenderId(), state.getCurrentTerm(),false));
            return;
        }

        int matchIndex;
        if (request.getEntries() != null && request.getEntries().size() > 0) {
            matchIndex = request.getPrevLogIndex() + request.getEntries().size();
            // update state
            log.updateLog(request.getPrevLogIndex(), request.getEntries());
            LOGGER.debug("Server {} received an append entries request and updated its log. The current matchIndex is {}!", context.getLocalId(), matchIndex);
        } else {
            matchIndex = request.getPrevLogIndex();
            LOGGER.trace("Server {} received a heartbeat. The current matchIndex is {}!", context.getLocalId(), matchIndex);

        }

        // update commit index
        if (log.getCommitIndex() < request.getLeaderCommitIndex()) {
            int newCommitIndex = Math.min(request.getLeaderCommitIndex(), matchIndex);
            LOGGER.debug("Server {} is committing entries from indices {} to {} after getting new commit index from leader", context.getLocalId(), log.getCommitIndex() + 1, newCommitIndex);
            List<LogEntry> committedEntries = log.updateCommitIndex(newCommitIndex);
            commitEntries(committedEntries);
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

                LOGGER.debug("Server {} got rejected append entries response from server {}, is trying again with decremented index!",
                        context.getLocalId(), response.getSenderId());
                state.decrementNextIndex(response.getSenderId());
                sendAppendEntriesRequest(response.getSenderId());
            }

            // if append entries request was successful, update the matchIndex and nextIndex for the follower
            if (response.isSuccess()) {

                LOGGER.trace("Server {} got successful append entries response from server {} with matchIndex {}!", context.getLocalId(), response.getSenderId(), response.getMatchIndex());

                state.updateMatchIndex(response.getSenderId(), response.getMatchIndex());
                state.updateNextIndex(response.getSenderId(), response.getMatchIndex() + 1);

                int newCommitIndex = state.getNewCommitIndex();
                int currentCommitIndex = log.getCommitIndex();
                if (newCommitIndex > currentCommitIndex) {

                    LOGGER.info("Server {} is now committing the log entries up to index {}!", context.getLocalId(), newCommitIndex);
                    List<LogEntry> committedEntries = log.updateCommitIndex(newCommitIndex);
                    commitEntries(committedEntries);

                    // send responses for every log entry that was handled by this server and is now committed
                    for (LogEntry entry: committedEntries) {
                        ClientResponse clientResponse = entry.buildResponse();

                        if (clientResponse != null) {
                            LOGGER.debug("Server {} is sending response to client {} for index {} because it was committed!", context.getLocalId(), clientResponse.getReceiverAddress(), log.indexOf(entry));

                            networkService.sendMessage(clientResponse);
                        } else {
                            LOGGER.error("Server {} wanted to send a response to client but the log entry was not applied");
                        }
                    }
                }
            }
        }

    }

    @Override
    public synchronized void processClientRequest(ClientRequest request) {

        // serve request if server is leader
        if (state.isLeader()) {

            if (request.isReadRequest()) {
                // TODO check with majority of servers if still leader
                LOGGER.debug("Server {} got read request!", context.getLocalId());

                ReadRequest readRequest = (ReadRequest) request;
                readRequest.commit(log.getStateMachine(), context);
                ClientResponse response = readRequest.buildResponse();
                networkService.sendMessage(response);
            } else {
                // check if request was already handled before
                if (log.contains(request)) {

                    LogEntry currentLogEntry = log.get(log.indexOf(request));
                    currentLogEntry.updateClientRequest(request);

                    // check if logEntry is already committed
                    if (currentLogEntry.isCommitted()) {
                        // if request is committed, get the response and send it
                        ClientResponse clientResponse = currentLogEntry.buildResponse();

                        if (clientResponse == null) {
                            // this should not happen
                            LOGGER.error("Server {} got request with already existent id which is not pending and has no response associated to it!", context.getLocalId());
                        } else {
                            networkService.sendMessage(clientResponse);
                        }
                    }
                } else {
                    request.onAppend(context, state);
                    log.append(request);

                    LOGGER.debug("Server {} got write request and is sending append entries requests to followers!", context.getLocalId());
                    // update logs of all servers
                    for (RaftID server : context.getOtherServerIds()) {
                        sendAppendEntriesRequest(server);
                    }
                }
            }

        // else redirect client to current leader
        } else {
            LOGGER.debug("Server {} got client request and is redirecting the client to server {}!", context.getLocalId(), state.getCurrentLeader());
            RaftID currentLeader = state.getCurrentLeader();
            networkService.sendMessage(new ClientRedirection(request.getSenderAddress(), currentLeader == null ? null : context.getAddressById(currentLeader)));
        }


    }

    @Override
    public void processAddServerRequest(AddServerRequest request) {

    }

    @Override
    public void processRemoveServerRequest(RemoveServerRequest request) {

    }

    @Override
    public synchronized void processTimeout() {
        LOGGER.trace("Server {} timed out as {}!", context.getLocalId(), state.getState());
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

        int prevLogTerm = state.getNextIndex(followerId) == 0 ? -1 : log.get(state.getNextIndex(followerId)-1).getTerm();

        AppendEntriesRequest request = new AppendEntriesRequest(followerId, state.getCurrentTerm(),
                state.getNextIndex(followerId) - 1, prevLogTerm, log.getCommitIndex(), log.getNewestEntries(state.getNextIndex(followerId)));
        networkService.sendMessage(request);
    }

    private void commitEntries(List<LogEntry> entries) {
        for (LogEntry entry: entries) {
            entry.commit(log.getStateMachine(), context);
        }
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
        if (args.length < 1) {
            System.out.println("Id must be provided");
            return;
        }

        short id = Short.parseShort(args[0]);
        List<RaftAddress> servers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
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

        server.start();
    }

    public static final class RaftServerBuilder {
        private ServerNetworkService networkService;
        private RaftServerContext context;
        private ServerState state;
        private Log log;
        private RaftTimer timer;

        private RaftServerBuilder() {
        }

        public static RaftServerBuilder aRaftServer() {
            return new RaftServerBuilder();
        }

        public RaftServerBuilder withNetworkService(ServerNetworkService networkService) {
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

        public RaftServerBuilder withTimer(RaftTimer timer) {
            this.timer = timer;
            return this;
        }

        public RaftServer build() {
            if (context == null) {
                throw new IllegalArgumentException("Context must be provided!");
            }

            if (log == null) {
                log = Log.LogBuilder.aLog().build();
            }

            if (networkService == null) {
                networkService = new DatagramNetworkService(context);
            }

            if (timer == null) {
                timer = new RaftTimer(context);
            }

            if (state == null) {
                state = new ServerState(context, timer, log);
            }

            return new RaftServer(networkService, context, state, log, timer);
        }
    }
}
