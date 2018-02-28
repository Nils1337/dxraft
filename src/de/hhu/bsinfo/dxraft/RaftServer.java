package de.hhu.bsinfo.dxraft;

import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.message.AppendEntriesRequest;
import de.hhu.bsinfo.dxraft.message.VoteRequest;
import de.hhu.bsinfo.dxraft.message.VoteResponse;
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

    /* volatile context */
    private int commitIndex = 0;
    private int lastApplied = 0;
    private int[] nextIndex;
    private int[] matchIndex;
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
    public void processVoteRequest(VoteRequest request) {
        if (request.getTerm() > currentTerm) {
            convertToFollower(request.getTerm());
        }

        VoteResponse response;
        if (votedFor == 0 || votedFor == request.getSenderId()) {
            //TODO check candidate log
            response = new VoteResponse(currentTerm, context.getLocalId(), request.getSenderId(), true);
            votedFor = request.getSenderId();
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
    }


    @Override
    public void processTimeout() {
        System.out.println("Server " + context.getLocalId() + " timed out as " + state.toString() + "!");

        if (state == RaftState.FOLLOWER || state == RaftState.CANDIDATE) {

            // server timed out as follower or candidate, so either the leader is not available or the election did not succeed
            // -> server starts new election and becomes candidate
            convertToCandidate();

            // send VoteRequests to all servers
            for (short id : context.getServers()) {
                VoteRequest voteRequest = new VoteRequest(currentTerm, context.getLocalId(), id, 0, 0);
                networkService.sendMessage(voteRequest);
            }
        } else {
            // server is leader, send heartbeat
            for (short id : context.getServers()) {
                AppendEntriesRequest voteRequest = new AppendEntriesRequest(currentTerm, context.getLocalId(), id);
                networkService.sendMessage(voteRequest);
            }

            // server is not changing state so timer has to be reset explicitly
            resetTimer();
        }

    }

    /**
     * Changes the state to Follower. This happens when a message with a higher term is received. Therefore the local currentTerm has to be updated.
     * Additionally the votedFor variable and the list with the collected votes from the earlier term are cleared.
     * @param newTerm new (higher) term that was received from another server
     */
    private void convertToFollower(int newTerm) {
        System.out.println("Server " + context.getLocalId() + " converting to Follower!");
        currentTerm = newTerm;
        state = RaftState.FOLLOWER;
        votedFor = 0;
        votes.clear();
        resetTimer();
    }

    /**
     * Changes the state to Candidate. This happens when the server times out as Follower. Therefore the local currentTerm is incremented.
     * Additionally the server votes for itself (the votedFor variable is set to the server's own id).
     */
    private void convertToCandidate() {
        System.out.println("Server " + context.getLocalId() + " converting to Candidate!");
        currentTerm++;
        state = RaftState.CANDIDATE;

        // not needed because already cleared when converted to follower
        // votes.clear();
        votedFor = context.getLocalId();
        resetTimer();
    }

    /**
     * Changes the state to Leader. This happens when the server got a quorum of servers that voted for it.
     */
    private void convertToLeader() {
        System.out.println("Server " + context.getLocalId() + " converting to Leader!");
        state = RaftState.LEADER;
        // not needed because votes are cleared when converting back to follower
        // votes.clear();
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
}
