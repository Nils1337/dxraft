package de.hhu.bsinfo.dxraft;

import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.log.Log;
import de.hhu.bsinfo.dxraft.message.*;
import de.hhu.bsinfo.dxraft.net.RaftMessageReceiver;
import de.hhu.bsinfo.dxraft.net.RaftNetworkService;
import de.hhu.bsinfo.dxraft.timer.RaftTimer;
import de.hhu.bsinfo.dxraft.timer.TimeoutHandler;

import java.util.HashMap;
import java.util.Map;

public class RaftServer implements RaftMessageReceiver, TimeoutHandler {

    /* persistent context */
    // TODO persist
    private RaftState state = RaftState.FOLLOWER;
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

    private RaftNetworkService networkService;
    private RaftContext context;
    private RaftTimer timer;

    private boolean started = false;

    public RaftServer(RaftContext context, RaftNetworkService networkService) {
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
    public void processMessage(RaftMessage message) {
        if (message.getTerm() > currentTerm) {

            increaseTerm(message.getTerm());

        }
    }


    @Override
    public void processVoteRequest(VoteRequest request) {

        VoteResponse response;
        if ( (votedFor == 0 || votedFor == request.getSenderId()) &&
                isLogAtLeastAsUpToDateAsLocalLog(request.getLastLogTerm(), request.getLastLogIndex())) {
            response = new VoteResponse(currentTerm, context.getLocalId(), request.getSenderId(), true);
            votedFor = request.getSenderId();
            resetTimer();
        } else {
            response = new VoteResponse(currentTerm, context.getLocalId(), request.getSenderId(),false);
        }

        networkService.sendMessage(response);
    }

    @Override
    public void processVoteResponse(VoteResponse response) {
        if (state == RaftState.CANDIDATE) {
            if (response.isVoteGranted()) {

                System.out.println("Server " + context.getLocalId() + " got vote from server " + response.getSenderId() + "!");
                votes.put(response.getSenderId(), true);

                //check if server got quorum
                if (votes.values().stream().filter((value) -> value).count() >= Math.ceil(context.getServerCount()/2)) {
                    convertToLeader();
                }
            } else {

                System.out.println("Server " + context.getLocalId() + " got rejection from server " + response.getSenderId() + "!");
                votes.put(response.getSenderId(), false);
            }
        }
    }

    @Override
    public void processAppendEntriesRequest(AppendEntriesRequest request) {
        resetTimer();

        if (currentTerm == request.getTerm() && state != RaftState.FOLLOWER) {
            convertToFollower();
        }

        // return false if request term < local term or if logs differ
        if (currentTerm > request.getTerm() || (request.getPrevLogIndex() >= 0 && log.get(request.getPrevLogIndex()).getTerm() != request.getPrevLogTerm())) {
            networkService.sendMessage(new AppendEntriesResponse(currentTerm, context.getLocalId(), request.getSenderId(), false));
            return;
        }

        int matchIndex;
        if (request.getEntries() != null && request.getEntries().size() > 0) {

            matchIndex = request.getPrevLogIndex() + request.getEntries().size();

            // update log
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
        networkService.sendMessage(new AppendEntriesResponse(currentTerm, context.getLocalId(), request.getSenderId(), true, matchIndex));

    }

    @Override
    public void processAppendEntriesResponse(AppendEntriesResponse response) {


        if (state == RaftState.LEADER) {
            // if append entries request was not successful and server is still leader, the log of the follower differs from the leader's log
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

                updateCommitIndex();
            }
        }

    }


    @Override
    public void processTimeout() {
        System.out.println("Server " + context.getLocalId() + " timed out as " + state.toString() + "!");

        if (state == RaftState.FOLLOWER) {

            // server timed out as follower, so the leader is not available
            // -> server becomes candidate and starts election
            convertToCandidate();

        } else if (state == RaftState.CANDIDATE) {

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
        for (short id : context.getServers()) {
            AppendEntriesRequest request = new AppendEntriesRequest(currentTerm, context.getLocalId(), id, log.getLastIndex(), log.isEmpty() ? -1 : log.getLastEntry().getTerm(), log.getCommitIndex(), null);
            networkService.sendMessage(request);
        }
    }

    private void sendAppendEntriesRequest(short followerId) {
        if (state != RaftState.LEADER) {
            throw new IllegalStateException("Append entries request could not be sent because state is " + state.toString() + " but should be LEADER!");
        }

        AppendEntriesRequest request = new AppendEntriesRequest(currentTerm, context.getLocalId(), followerId,
                nextIndex.get(followerId) - 1, log.get(nextIndex.get(followerId)-1).getTerm(), log.getCommitIndex(), log.getNewestEntries(nextIndex.get(followerId)));
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
        if (state == RaftState.FOLLOWER) {
            throw new IllegalStateException("Server could not convert to follower because state is " + state.toString() + " but should be CANDIDATE or LEADER!");
        }

        System.out.println(message);
        state = RaftState.FOLLOWER;
        votedFor = 0;
        resetTimer();
    }

    /**
     * Changes the state to Candidate. This happens when the server times out as Follower.
     */
    private void convertToCandidate() {
        if (state != RaftState.FOLLOWER) {
            throw new IllegalStateException("Server could not convert to candidate because state is " + state.toString() + " but should be FOLLOWER!");
        }

        System.out.println("Server " + context.getLocalId() + " converting to Candidate!");
        state = RaftState.CANDIDATE;
        startNewElection();
    }

    /**
     * Starts a new election. Current term is incremented, vote requests are sent to all servers and timer is reset.
     * @throws IllegalStateException if server is not in candidate state
     */
    private void startNewElection() {
        if (state != RaftState.CANDIDATE) {
            throw new IllegalStateException("Election could not be started because state is " + state.toString() + " but should be CANDIDATE!");
        }

        currentTerm++;
        System.out.println("Server " + context.getLocalId() + " starting new election in term " + currentTerm + ".");
        votes.clear();
        votedFor = context.getLocalId();
        sendVoteRequests();
        resetTimer();
    }

    /**
     * Sends vote requests to all servers.
     */
    private void sendVoteRequests() {
        for (short id : context.getServers()) {
            VoteRequest voteRequest = new VoteRequest(currentTerm, context.getLocalId(), id, 0, 0);
            networkService.sendMessage(voteRequest);
        }
    }

    /**
     * Changes the state to Leader. This happens when the server got a quorum of servers that voted for it.
     */
    private void convertToLeader() {
        if (state != RaftState.CANDIDATE) {
            throw new IllegalStateException("Server could not convert to leader because state is " + state.toString() + " but should be CANDIDATE!");
        }

        System.out.println("Server " + context.getLocalId() + " converting to Leader in term " + currentTerm + "!");
        state = RaftState.LEADER;

        for (short serverId : context.getServers()) {
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
     * Checks if other log is at least as up to date as the local log. This is the case if the term of the last log entry of the other log is
     * higher than the last log entry of the local log or if
     * the term is the same and the other log is at least as long as the local log
     * @param lastTerm term of the last log entry of the other log
     * @param lastIndex last index of the other log
     * @return true if the other log is at least as up to date as the local log
     */
    private boolean isLogAtLeastAsUpToDateAsLocalLog(int lastTerm, int lastIndex) {
        return log.isEmpty() || lastTerm > log.getLastEntry().getTerm() || (lastTerm == log.getLastEntry().getTerm() && lastIndex >= log.getLastIndex());
    }

    /**
     * Updates the commit index when being leader. Checks the matchIndexes of the followers to find a higher index where the logs of a majority of the servers
     * match and the term is the current term of the leader. Does nothing if there is none such index.
     */
    private void updateCommitIndex() {
        int newCommitIndex = 0;
        for (int i = log.getCommitIndex() + 1; i <= log.getLastIndex(); i++) {
            final int index = i;
            if (matchIndex.values().stream().filter(matchIndex -> matchIndex >= index).count() >= Math.ceil(context.getServerCount()/2) && log.get(i).getTerm() == currentTerm) {
                newCommitIndex = index;
            }
        }

        if (newCommitIndex != 0) {
            log.setCommitIndex(newCommitIndex);
        }
    }

    private void increaseTerm(int newTerm) {
        // if server receives message with higher term it has to convert to follower
        // if it already is a follower, only reset the timer and clear the current vote
        if (state != RaftState.FOLLOWER) {
            convertToFollower(currentTerm, newTerm);
        } else {
            resetTimer();
            votedFor = 0;
        }
        currentTerm = newTerm;
    }
}
