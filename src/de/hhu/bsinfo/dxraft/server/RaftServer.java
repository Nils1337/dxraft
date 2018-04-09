package de.hhu.bsinfo.dxraft.server;

import de.hhu.bsinfo.dxraft.state.Log;
import de.hhu.bsinfo.dxraft.message.*;
import de.hhu.bsinfo.dxraft.state.LogEntry;
import de.hhu.bsinfo.dxraft.timer.RaftTimer;
import de.hhu.bsinfo.dxraft.timer.TimeoutHandler;

import java.util.HashMap;
import java.util.Map;

public class RaftServer implements ServerMessageReceiver, TimeoutHandler {

    public static final boolean electionDebug = false;
    public static final boolean clientRequestDebug = false;

    private ServerNetworkService networkService;
    private RaftServerContext context;
    private ServerState state;
    private Log log = new Log();

    private boolean started = false;
    private Map<LogEntry, ClientRequest> pendingRequests = new HashMap<>();

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
        if ((state.getVotedFor() == 0 || state.getVotedFor() == request.getSenderId()) &&
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

                if (electionDebug) {
                    System.out.println("Server " + context.getLocalId() + " got vote from server " + response.getSenderId() + "!");
                }

                state.getVotesMap().put(response.getSenderId(), true);

                //check if server got quorum
                if (state.getVotesMap().values().stream().filter((value) -> value).count() >= Math.ceil(context.getRaftServers().size()/2)) {
                    state.convertStateToLeader();
                    sendHeartbeat();
                }
            } else {

                if (electionDebug) {
                    System.out.println("Server " + context.getLocalId() + " got rejection from server " + response.getSenderId() + "!");
                }
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
                if (electionDebug) {
                    System.out.println("Server " + context.getLocalId() + " terminating election because got request from leader in current term " + state.getCurrentTerm() + "!");
                }
                state.convertStateToFollower();;
            }

            state.updateLeader(request.getSenderId());
        }


        // return false if request term < local term or if logs differ
        if (state.getCurrentTerm() > request.getTerm() || log.getSize() <= request.getPrevLogIndex() || (!log.isEmpty() && (request.getPrevLogIndex() >= 0 && log.get(request.getPrevLogIndex()).getTerm() != request.getPrevLogTerm()))) {

            if (clientRequestDebug) {
                System.out.println("Server " + context.getLocalId() + " is rejecting append entries request because it is an old request or the logs differ!");
            }

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

            if (clientRequestDebug) {
                System.out.println("Server " + context.getLocalId() + " received an append entries request and updated its log. The current matchIndex is " + matchIndex + "!");
            }
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

                if (clientRequestDebug) {
                    System.out.println("Server " + context.getLocalId() + " got rejected append entries response from server " + response.getSenderId() +", is trying again with decremented index!");
                }
                state.getNextIndexMap().put(response.getSenderId(), state.getNextIndexMap().get(response.getSenderId()) - 1);
                sendAppendEntriesRequest(response.getSenderId());
            }

            // if append entries request was successful, update the matchIndex and nextIndex for the follower
            if (response.isSuccess()) {

                /*if (clientRequestDebug) {
                    System.out.println("Server " + context.getLocalId() + " got successful append entries response from server " + response.getSenderId() +" with matchIndex "+ response.getMatchIndex() + "!");
                }*/
                // TODO correct?
                state.getMatchIndexMap().put(response.getSenderId(), response.getMatchIndex());
                state.getNextIndexMap().put(response.getSenderId(), response.getMatchIndex() + 1);

                int newCommitIndex = getNewCommitIndex();
                int currentCommitIndex = log.getCommitIndex();
                if (newCommitIndex > currentCommitIndex) {

                    if (clientRequestDebug) {
                        System.out.println("Server " + context.getLocalId() + " is now committing the log entries up to index " + newCommitIndex + "!");
                    }
                    log.updateCommitIndex(newCommitIndex);

                    // send responses for every log entry that was handled by this server and is now committed
                    for (int i = currentCommitIndex + 1; i <= newCommitIndex; i++) {
                        LogEntry logEntry = log.get(i);
                        ClientRequest request = pendingRequests.remove(logEntry);

                        if (request != null) {
                            if (clientRequestDebug) {
                                System.out.println("Server " + context.getLocalId() + " is sending response to client " + request.getSenderId() + " for index "+ i + " because it was committed!");
                            }

                            ClientResponse clientResponse;
                            if (request.isDeleteRequest()) {
                                clientResponse = new ClientResponse(context.getLocalId(), request.getSenderId(), logEntry.getValue());
                            } else {
                                clientResponse = new ClientResponse(context.getLocalId(), request.getSenderId(), true);
                            }
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
                if (clientRequestDebug) {
                    System.out.println("Server " + context.getLocalId() + " got read request!");
                }

                Object value = log.getStateMachine().read(request.getPath());
                ClientResponse response = new ClientResponse(context.getLocalId(), request.getSenderId(), value);
                networkService.sendMessage(response);
            } else if (request.isWriteRequest() || request.isDeleteRequest()) {
                // update log
                LogEntry logEntry = new LogEntry(state.getCurrentTerm(), request.getPath(), request.isWriteRequest() ? LogEntry.LogEntryType.PUT : LogEntry.LogEntryType.DELETE, request.getValue());
                log.append(logEntry);

                // save request to later send response
                pendingRequests.put(logEntry, request);

                if (clientRequestDebug) {
                    System.out.println("Server " + context.getLocalId() + " got write request and is sending append entries requests to followers!");
                }
                // update logs of all servers
                for (short server : context.getRaftServers()) {
                    sendAppendEntriesRequest(server);
                }
            }

        // else redirect client to current leader
        } else {
            if (clientRequestDebug) {
                System.out.println("Server " + context.getLocalId() + " got client request and is redirecting the client to server " + state.getCurrentLeader() +"!");
            }
            networkService.sendMessage(new ClientRedirection(context.getLocalId(), request.getSenderId(), state.getCurrentLeader()));
        }


    }

    @Override
    public void processTimeout() {
        /*if (debug) {
            System.out.println("Server " + context.getLocalId() + " timed out as " + state.getState() + "!");
        }*/

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
        for (short id : context.getRaftServers()) {
            AppendEntriesRequest request = new AppendEntriesRequest(context.getLocalId(), id, state.getCurrentTerm(), log.getLastIndex(), log.isEmpty() ? -1 : log.getLastEntry().getTerm(), log.getCommitIndex(), null);
            networkService.sendMessage(request);
        }
    }

    private void sendAppendEntriesRequest(short followerId) {
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
        for (short id : context.getRaftServers()) {
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
