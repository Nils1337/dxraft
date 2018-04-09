package de.hhu.bsinfo.dxraft.server;

import de.hhu.bsinfo.dxraft.state.Log;
import de.hhu.bsinfo.dxraft.message.*;
import de.hhu.bsinfo.dxraft.state.LogEntry;
import de.hhu.bsinfo.dxraft.state.LogEntryType;
import de.hhu.bsinfo.dxraft.timer.RaftTimer;
import de.hhu.bsinfo.dxraft.timer.TimeoutHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaftServer implements ServerMessageReceiver, TimeoutHandler {

    public static final boolean debug = false;

    /* persistent context */
    // TODO persist
    private ServerState state = ServerState.FOLLOWER;
    private int currentTerm = 0;

    // TODO can servers have id 0?
    private short votedFor = 0;
    private Log log = new Log();

    /* volatile context */
    //private int commitIndex = 0;
    //private int lastApplied = 0;
    private Map<Short, Integer> nextIndex = new HashMap<>();
    private Map<Short, Integer> matchIndex = new HashMap<>();
    private Map<Short, Boolean> votes = new HashMap<>();

    private ServerNetworkService networkService;
    private RaftServerContext context;
    private RaftTimer timer;

    private short currentLeader = 0;
    private boolean started = false;
    private Map<LogEntry, ClientRequest> pendingRequests = new HashMap<>();

    public RaftServer(RaftServerContext context, ServerNetworkService networkService) {
        this.networkService = networkService;
        networkService.setMessageReceiver(this);
        this.context = context;
        this.timer = new RaftTimer(context,this);
    }

    public void start() {
        if (!started) {
            resetTimer();
            this.networkService.start();
        }
    }


    @Override
    public void processVoteRequest(VoteRequest request) {
        checkTerm(request);

        VoteResponse response;
        if ( (votedFor == 0 || votedFor == request.getSenderId()) &&
                isLogAtLeastAsUpToDateAsLocalLog(request.getLastLogTerm(), request.getLastLogIndex())) {
            response = new VoteResponse(context.getLocalId(), request.getSenderId(), currentTerm, true);
            votedFor = request.getSenderId();
            resetTimer();
        } else {
            response = new VoteResponse(context.getLocalId(), request.getSenderId(), currentTerm, false);
        }

        networkService.sendMessage(response);
    }

    @Override
    public void processVoteResponse(VoteResponse response) {
        checkTerm(response);

        if (state == ServerState.CANDIDATE) {
            if (response.isVoteGranted()) {

                if (debug) {
                    System.out.println("Server " + context.getLocalId() + " got vote from server " + response.getSenderId() + "!");
                }

                votes.put(response.getSenderId(), true);

                //check if server got quorum
                if (votes.values().stream().filter((value) -> value).count() >= Math.ceil(context.getRaftServers().size()/2)) {
                    convertToLeader();
                }
            } else {

                if (debug) {
                    System.out.println("Server " + context.getLocalId() + " got rejection from server " + response.getSenderId() + "!");
                }
                votes.put(response.getSenderId(), false);
            }
        }
    }

    @Override
    public void processAppendEntriesRequest(AppendEntriesRequest request) {
        checkTerm(request);
        resetTimer();

        if (currentTerm <= request.getTerm()) {
            currentLeader = request.getSenderId();
        }

        // if server is candidate and receives an append entries request with its current term
        // it means a leader was already chosen and the server should convert back to follower
        if (currentTerm == request.getTerm() && state == ServerState.CANDIDATE) {
            convertToFollower();
        }

        // return false if request term < local term or if logs differ
        if (currentTerm > request.getTerm() || log.getSize() <= request.getPrevLogIndex() || (!log.isEmpty() && (request.getPrevLogIndex() >= 0 && log.get(request.getPrevLogIndex()).getTerm() != request.getPrevLogTerm()))) {
            networkService.sendMessage(new AppendEntriesResponse(context.getLocalId(), request.getSenderId(), currentTerm,false));
            return;
        }

        int matchIndex;
        if (request.getEntries() != null && request.getEntries().size() > 0) {

            matchIndex = request.getPrevLogIndex() + request.getEntries().size();

            // update state
            log.updateLog(request.getPrevLogIndex(), request.getEntries());

            // update commitIndex
            if (log.getCommitIndex() < request.getLeaderCommitIndex()) {
                log.setCommitIndex(Math.min(request.getLeaderCommitIndex(), request.getPrevLogIndex() + request.getEntries().size()));
            }
        } else {
            matchIndex = request.getPrevLogIndex();

            if (log.getCommitIndex() < request.getLeaderCommitIndex()) {
                log.setCommitIndex(Math.min(request.getLeaderCommitIndex(), request.getPrevLogIndex()));
            }
        }

        //send response
        networkService.sendMessage(new AppendEntriesResponse(context.getLocalId(), request.getSenderId(),currentTerm, true, matchIndex));

    }

    @Override
    public void processAppendEntriesResponse(AppendEntriesResponse response) {
        checkTerm(response);

        if (state == ServerState.LEADER) {
            // if append entries request was not successful and server is still leader, the state of the follower differs from the leader's state
            // -> decrease nextIndex of the follower and try again
            if (!response.isSuccess() && nextIndex.get(response.getSenderId()) > 0) {
                nextIndex.put(response.getSenderId(), nextIndex.get(response.getSenderId()) - 1);
                sendAppendEntriesRequest(response.getSenderId());
            }

            // if append entries request was successful, update the matchIndex and nextIndex for the follower
            if (response.isSuccess()) {
                // TODO correct?
                matchIndex.put(response.getSenderId(), response.getMatchIndex());
                nextIndex.put(response.getSenderId(), response.getMatchIndex() + 1);

                int newCommitIndex = getNewCommitIndex();
                int currentCommitIndex = log.getCommitIndex();
                if (newCommitIndex > currentCommitIndex) {
                    log.setCommitIndex(newCommitIndex);

                    // send responses for every log entry that was handled by this server and is now committed
                    for (int i = currentCommitIndex; i <= newCommitIndex; i++) {
                        LogEntry logEntry = log.get(i);
                        ClientRequest request = pendingRequests.remove(logEntry);

                        if (request != null) {
                            networkService.sendMessage(new ClientResponse(context.getLocalId(), request.getSenderId(), logEntry.getValue()));
                        }
                    }
                }
            }
        }

    }

    @Override
    public void processClientRequest(ClientRequest request) {

        // serve request if server is leader
        if (state == ServerState.LEADER) {

            // TODO check with majority of servers if still leader?

            if (request.getRequestType() == ClientRequest.RequestType.GET) {
                Object value = log.getStateMachine().read(request.getPath());
                ClientResponse response = new ClientResponse(context.getLocalId(), request.getSenderId(), value);
                networkService.sendMessage(response);
            } else if (request.getRequestType() == ClientRequest.RequestType.PUT) {
                // update log
                LogEntry logEntry = new LogEntry(currentTerm, request.getPath(), LogEntryType.PUT, request.getValue());
                log.append(logEntry);

                // save request to later send response
                pendingRequests.put(logEntry, request);

                // update logs of all servers
                for (short server : context.getRaftServers()) {
                    sendAppendEntriesRequest(server);
                }
            }

        // else redirect client to current leader
        } else {
            networkService.sendMessage(new ClientRedirection(context.getLocalId(), request.getSenderId(), currentLeader));
        }


    }

    @Override
    public void processTimeout() {
        if (debug) {
            System.out.println("Server " + context.getLocalId() + " timed out as " + state.toString() + "!");
        }

        if (state == ServerState.FOLLOWER) {

            // server timed out as follower, so the leader is not available
            // -> server becomes candidate and starts election
            convertToCandidate();

        } else if (state == ServerState.CANDIDATE) {

            // server timed out as candidate, election was not successful
            // -> start new election
            startNewElection();

        } else {
            // server is leader, send heartbeat
            sendHeartbeat();

            // server is not changing state so timer has to be reset explicitly
            resetTimer();
        }

    }

    /**
     * Sends heartbeats to all servers.
     */
    private void sendHeartbeat() {
        for (short id : context.getRaftServers()) {
            AppendEntriesRequest request = new AppendEntriesRequest(context.getLocalId(), id, currentTerm, log.getLastIndex(), log.isEmpty() ? -1 : log.getLastEntry().getTerm(), log.getCommitIndex(), null);
            networkService.sendMessage(request);
        }
    }

    private void sendAppendEntriesRequest(short followerId) {
        if (state != ServerState.LEADER) {
            throw new IllegalStateException("Append entries request could not be sent because state is " + state.toString() + " but should be LEADER!");
        }

        int prevLogTerm = nextIndex.get(followerId) == 0 ? -1 : log.get(nextIndex.get(followerId)-1).getTerm();

        AppendEntriesRequest request = new AppendEntriesRequest(context.getLocalId(), followerId, currentTerm,
                nextIndex.get(followerId) - 1, prevLogTerm, log.getCommitIndex(), log.getNewestEntries(nextIndex.get(followerId)));
        networkService.sendMessage(request);
    }

    private void convertToFollower(int oldTerm, int newTerm) {
        convertToFollower("Server " + context.getLocalId() + " converting to Follower because local term was " + oldTerm + " and got message with term " + newTerm + "!");
    }

    private void convertToFollower() {
        convertToFollower("Server " + context.getLocalId() + " converting to Follower because it got message from leader in term " + currentTerm + "!");
    }

    /**
     * Changes the state to Follower. This happens when a message with a higher term is received.
     */
    private void convertToFollower(String message) {
        if (state == ServerState.FOLLOWER) {
            throw new IllegalStateException("Server could not convert to follower because state is " + state.toString() + " but should be CANDIDATE or LEADER!");
        }

        if (debug) {
            System.out.println(message);
        }
        state = ServerState.FOLLOWER;
        votedFor = 0;
        resetTimer();
    }

    /**
     * Changes the state to Candidate. This happens when the server times out as Follower.
     */
    private void convertToCandidate() {
        if (state != ServerState.FOLLOWER) {
            throw new IllegalStateException("Server could not convert to candidate because state is " + state.toString() + " but should be FOLLOWER!");
        }

        if (debug) {
            System.out.println("Server " + context.getLocalId() + " converting to Candidate!");
        }
        state = ServerState.CANDIDATE;
        startNewElection();
    }

    /**
     * Starts a new election. Current term is incremented, vote requests are sent to all servers and timer is reset.
     * @throws IllegalStateException if server is not in candidate state
     */
    private void startNewElection() {
        if (state != ServerState.CANDIDATE) {
            throw new IllegalStateException("Election could not be started because state is " + state.toString() + " but should be CANDIDATE!");
        }

        currentTerm++;
        currentLeader = 0;

        if (debug) {
            System.out.println("Server " + context.getLocalId() + " starting new election in term " + currentTerm + ".");
        }
        votes.clear();
        votedFor = context.getLocalId();
        sendVoteRequests();
        resetTimer();
    }

    /**
     * Sends vote requests to all servers.
     */
    private void sendVoteRequests() {
        for (short id : context.getRaftServers()) {
            VoteRequest voteRequest = new VoteRequest(context.getLocalId(), id, currentTerm, 0, 0);
            networkService.sendMessage(voteRequest);
        }
    }

    /**
     * Changes the state to Leader. This happens when the server got a quorum of servers that voted for it.
     */
    private void convertToLeader() {
        if (state != ServerState.CANDIDATE) {
            throw new IllegalStateException("Server could not convert to leader because state is " + state.toString() + " but should be CANDIDATE!");
        }

        System.out.println("Server " + context.getLocalId() + " converting to Leader in term " + currentTerm + "!");

        state = ServerState.LEADER;

        for (short serverId : context.getRaftServers()) {
            nextIndex.put(serverId, log.getLastIndex() + 1);
            matchIndex.put(serverId, 0);
        }

        // send initial heartbeat
        sendHeartbeat();
        resetTimer();
    }

    /**
     * Resets the timer. The timeout duration is dependent on the current status.
     */
    private void resetTimer() {
        timer.cancel();

        switch (state) {
            case FOLLOWER:
                timer.schedule(context.getFollowerTimeoutDuration(), context.getFollowerRandomizationAmount());
                break;
            case CANDIDATE:
                timer.schedule(context.getElectionTimeoutDuration(), context.getElectionRandomizationAmount());
                break;
            case LEADER:
                timer.schedule(context.getHeartbeatTimeoutDuration(), context.getHeartbeatRandomizationAmount());
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
            if (matchIndex.values().stream().filter(matchIndex -> matchIndex >= index).count() >= Math.ceil(context.getRaftServers().size()/2) && log.get(i).getTerm() == currentTerm) {
                newCommitIndex = index;
            }
        }
        return newCommitIndex;
    }

    /**
     * Checks if the term of the message is higher than the local term. If this is the case, state is changed to follower.
     * @param message
     */
    private void checkTerm(RaftServerMessage message) {
        if (message.getTerm() > currentTerm) {
            int newTerm = message.getTerm();
            // if server receives message with higher term it has to convert to follower
            // if it already is a follower, only reset the timer and clear the current vote
            if (state != ServerState.FOLLOWER) {
                convertToFollower(currentTerm, newTerm);
            } else {
                resetTimer();
                votedFor = 0;
            }
            currentTerm = newTerm;
            if (message instanceof AppendEntriesRequest) {
                currentLeader = message.getSenderId();
            } else {
                currentLeader = 0;
            }
        }
    }
}
