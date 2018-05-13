package de.hhu.bsinfo.dxraft.server;

import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.state.Log;
import de.hhu.bsinfo.dxraft.message.*;
import de.hhu.bsinfo.dxraft.state.LogEntry;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.timer.RaftTimer;
import de.hhu.bsinfo.dxraft.timer.TimeoutHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class RaftServer implements ServerMessageReceiver, TimeoutHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final boolean electionDebug = false;
    public static final boolean clientRequestDebug = false;

    private ServerNetworkService networkService;
    private RaftServerContext context;
    private ServerState state;
    private Log log = new Log();

    private boolean started = false;
    private ArrayList<LogEntry> pendingRequests = new ArrayList<>();

    public RaftServer(RaftServerContext context, ServerNetworkService networkService) {
        this.networkService = networkService;
        networkService.setMessageReceiver(this);
        this.context = context;
        RaftTimer timer = new RaftTimer(context,this);
        state = new ServerState(context, timer, log);
    }

    public void start() {
        if (!started) {
            state.startTimer();
            this.networkService.start();

            /*if (context.getLocalId() == 1) {
                state.convertStateToCandidate();
                sendVoteRequests();
            }*/

        }
    }


    @Override
    public void processVoteRequest(VoteRequest request) {
        state.checkTerm(request.getTerm());

        VoteResponse response;
        if ((state.getVotedFor() == null || state.getVotedFor() == request.getSenderId()) &&
                isLogAtLeastAsUpToDateAsLocalLog(request.getLastLogTerm(), request.getLastLogIndex())) {
            response = new VoteResponse(context.getLocalId(), request.getSenderId(), state.getCurrentTerm(), true);
            state.updateVote(request.getSenderId());

        } else {
            response = new VoteResponse(context.getLocalId(), request.getSenderId(), state.getCurrentTerm(), false);
        }

        networkService.sendMessage(response);
    }

    @Override
    public void processVoteResponse(VoteResponse response) {
        state.checkTerm(response.getTerm());

        if (state.isCandidate()) {
            if (response.isVoteGranted()) {

                LOGGER.debug("Server {} got vote from server {}!", context.getLocalId(), response.getSenderId());
                state.getVotesMap().put(response.getSenderId(), true);

                //check if server got quorum
                if (state.getVotesMap().values().stream().filter((value) -> value).count() >= Math.ceil(context.getRaftServers().size()/2)) {
                    state.convertStateToLeader();
                    sendHeartbeat();
                }
            } else {

                LOGGER.debug("Server {} got rejection from server {}!", context.getLocalId(), response.getSenderId());
                state.getVotesMap().put(response.getSenderId(), false);
            }
        }
    }

    @Override
    public void processAppendEntriesRequest(AppendEntriesRequest request) {
        state.checkTerm(request.getTerm(), request.getSenderId());
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
        if (state.getCurrentTerm() > request.getTerm() || log.getSize() <= request.getPrevLogIndex() || (!log.isEmpty() && (request.getPrevLogIndex() >= 0 && log.get(request.getPrevLogIndex()).getTerm() != request.getPrevLogTerm()))) {


            LOGGER.debug("Server {} is rejecting append entries request because it is an old request or the logs differ!", context.getLocalId());
            networkService.sendMessage(new AppendEntriesResponse(context.getLocalId(), request.getSenderId(), state.getCurrentTerm(),false));
            return;
        }

        int matchIndex;
        if (request.getEntries() != null && request.getEntries().size() > 0) {

            matchIndex = request.getPrevLogIndex() + request.getEntries().size();

            // update state
            log.updateLog(request.getPrevLogIndex(), request.getEntries());

            // update commitIndex
            if (log.getCommitIndex() < request.getLeaderCommitIndex()) {
                log.updateCommitIndex(Math.min(request.getLeaderCommitIndex(), request.getPrevLogIndex() + request.getEntries().size()));
            }

            LOGGER.debug("Server {} received an append entries request and updated its log. The current matchIndex is {}!", context.getLocalId(), matchIndex);
        } else {
            matchIndex = request.getPrevLogIndex();

            if (log.getCommitIndex() < request.getLeaderCommitIndex()) {
                log.updateCommitIndex(Math.min(request.getLeaderCommitIndex(), request.getPrevLogIndex()));
            }
        }

        //send response
        networkService.sendMessage(new AppendEntriesResponse(context.getLocalId(), request.getSenderId(), state.getCurrentTerm(), true, matchIndex));

    }

    @Override
    public void processAppendEntriesResponse(AppendEntriesResponse response) {
        state.checkTerm(response.getTerm());

        if (state.isLeader()) {
            // if append entries request was not successful and server is still leader, the state of the follower differs from the leader's state
            // -> decrease nextIndex of the follower and try again
            if (!response.isSuccess() && state.getNextIndexMap().get(response.getSenderId()) > 0) {

                LOGGER.debug("Server {} got rejected append entries response from server {}, is trying again with decremented index!",
                        context.getLocalId(), response.getSenderId());
                state.getNextIndexMap().put(response.getSenderId(), state.getNextIndexMap().get(response.getSenderId()) - 1);
                sendAppendEntriesRequest(response.getSenderId());
            }

            // if append entries request was successful, update the matchIndex and nextIndex for the follower
            if (response.isSuccess()) {

                LOGGER.trace("Server {} got successful append entries response from server {} with matchIndex {}!", context.getLocalId(), response.getSenderId(), response.getMatchIndex());
                // TODO correct?
                state.getMatchIndexMap().put(response.getSenderId(), response.getMatchIndex());
                state.getNextIndexMap().put(response.getSenderId(), response.getMatchIndex() + 1);

                int newCommitIndex = getNewCommitIndex();
                int currentCommitIndex = log.getCommitIndex();
                if (newCommitIndex > currentCommitIndex) {

                    LOGGER.info("Server {} is now committing the log entries up to index {}!", context.getLocalId(), newCommitIndex);

                    log.updateCommitIndex(newCommitIndex);

                    // send responses for every log entry that was handled by this server and is now committed
                    for (int i = currentCommitIndex + 1; i <= newCommitIndex; i++) {
                        LogEntry logEntry = log.get(i);
                        pendingRequests.remove(logEntry);
                        ClientRequest request = logEntry.getClientRequest();

                        if (request != null) {
                            LOGGER.debug("Server {} is sending response to client {} for index {} because it was committed!", context.getLocalId(), request.getSenderId(), i);

                            ClientResponse clientResponse = new ClientResponse(context.getLocalId(), request.getSenderId(), true);

                            logEntry.setClientResponse(clientResponse);
                            networkService.sendMessage(clientResponse);
                        }
                    }
                }
            }
        }

    }

    @Override
    public void processClientRequest(ClientRequest request) {

        // serve request if server is leader
        if (state.isLeader()) {

            // TODO check with majority of servers if still leader?

            if (request.isReadRequest()) {
                LOGGER.debug("Server {} got read request!", context.getLocalId());
                RaftData value = log.getStateMachine().read(request.getPath());
                ClientResponse response = new ClientResponse(context.getLocalId(), request.getSenderId(), value);
                networkService.sendMessage(response);
            } else if (request.isWriteRequest() || request.isDeleteRequest()) {
                // update log
                LogEntry logEntry = new LogEntry(state.getCurrentTerm(), request);

                // check if request was already handled before
                if (log.contains(logEntry)) {

                    // check if logEntry is a pending request
                    if (pendingRequests.contains(logEntry)) {
                        // if request is pending, update the request of the log entry and wait for the request to finish
                        pendingRequests.get(pendingRequests.indexOf(logEntry)).setClientRequest(request);
                    } else {
                        // if request is not pending, get the response that was sent and send it again
                        LogEntry currentLogEntry = log.get(log.indexOf(logEntry));
                        ClientResponse clientResponse = currentLogEntry.getClientResponse();

                        if (clientResponse == null) {
                            // this should not happen
                            LOGGER.error("Server {} got request with already existent id which is not pending and has no response associated to it!", context.getLocalId());
                        } else {
                            networkService.sendMessage(clientResponse);
                        }
                    }
                } else {
                    log.append(logEntry);

                    // save request to later send response
                    pendingRequests.add(logEntry);

                    LOGGER.debug("Server {} got write request and is sending append entries requests to followers!", context.getLocalId());
                    // update logs of all servers
                    for (RaftID server : context.getRaftServers()) {
                        sendAppendEntriesRequest(server);
                    }
                }
            }

        // else redirect client to current leader
        } else {
            LOGGER.debug("Server {} got client request and is redirecting the client to server {}!", context.getLocalId(), state.getCurrentLeader());
            networkService.sendMessage(new ClientRedirection(context.getLocalId(), request.getSenderId(), state.getCurrentLeader()));
        }


    }

    @Override
    public void processTimeout() {
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
        for (RaftID id : context.getRaftServers()) {
            AppendEntriesRequest request = new AppendEntriesRequest(context.getLocalId(), id, state.getCurrentTerm(), log.getLastIndex(), log.isEmpty() ? -1 : log.getLastEntry().getTerm(), log.getCommitIndex(), null);
            networkService.sendMessage(request);
        }
    }

    private void sendAppendEntriesRequest(RaftID followerId) {
        if (!state.isLeader()) {
            throw new IllegalStateException("Append entries request could not be sent because state is " + state.getState() + " but should be LEADER!");
        }

        int prevLogTerm = state.getNextIndexMap().get(followerId) == 0 ? -1 : log.get(state.getNextIndexMap().get(followerId)-1).getTerm();

        AppendEntriesRequest request = new AppendEntriesRequest(context.getLocalId(), followerId, state.getCurrentTerm(),
                state.getNextIndexMap().get(followerId) - 1, prevLogTerm, log.getCommitIndex(), log.getNewestEntries(state.getNextIndexMap().get(followerId)));
        networkService.sendMessage(request);
    }

    /**
     * Sends vote requests to all servers.
     */
    private void sendVoteRequests() {
        for (RaftID id : context.getRaftServers()) {
            VoteRequest voteRequest = new VoteRequest(context.getLocalId(), id, state.getCurrentTerm(), log.getLastIndex(), log.getLastIndex() >= 0 ? log.getLastEntry().getTerm() : -1);
            networkService.sendMessage(voteRequest);
        }
    }

    /**
     * Checks if other state is at least as up to date as the local state. This is the case if the term of the last state entry of the other state is
     * higher than the last state entry of the local state or if
     * the term is the same and the other state is at least as long as the local state
     * @param lastTerm term of the last state entry of the other state
     * @param lastIndex last index of the other state
     * @return true if the other state is at least as up to date as the local state
     */
    private boolean isLogAtLeastAsUpToDateAsLocalLog(int lastTerm, int lastIndex) {
        return log.isEmpty() || lastTerm > log.getLastEntry().getTerm() || (lastTerm == log.getLastEntry().getTerm() && lastIndex >= log.getLastIndex());
    }

    /**
     * Checks the matchIndexes of the followers to find a higher index where the logs of a majority of the servers match and
     * the term is the current term of the leader and returns this index. Returns the current commit index if there is no such index.
     */
    private int getNewCommitIndex() {
        int newCommitIndex = log.getCommitIndex();
        for (int i = log.getCommitIndex() + 1; i <= log.getLastIndex(); i++) {
            final int index = i;
            if (state.getMatchIndexMap().values().stream().filter(matchIndex -> matchIndex >= index).count() >= Math.ceil(context.getRaftServers().size()/2) && log.get(i).getTerm() == state.getCurrentTerm()) {
                newCommitIndex = index;
            }
        }
        return newCommitIndex;
    }
}
