package de.hhu.bsinfo.dxraft.server;

import de.hhu.bsinfo.dxraft.state.Log;
import de.hhu.bsinfo.dxraft.timer.RaftTimer;

import java.util.HashMap;
import java.util.Map;

public class ServerState {

    private enum State {
        FOLLOWER, CANDIDATE, LEADER
    }

    /* persistent context */
    // TODO persist
    private State state = State.FOLLOWER;
    private int currentTerm = 0;

    /* volatile context */
    //private int lastApplied = 0;

    // TODO can servers have id 0?
    private short votedFor = 0;
    private short currentLeader = 0;

    private RaftTimer timer;
    private RaftServerContext context;

    private Map<Short, Integer> nextIndexMap = new HashMap<>();
    private Map<Short, Integer> matchIndexMap = new HashMap<>();
    private Map<Short, Boolean> votesMap = new HashMap<>();
    private Log log;

    public ServerState(RaftServerContext context, RaftTimer timer, Log log) {
        this.context = context;
        this.timer = timer;
        this.log = log;
    }

    public boolean isFollower() {
        return state == State.FOLLOWER;
    }

    public boolean isCandidate() {
        return state == State.CANDIDATE;
    }

    public boolean isLeader() {
        return state == State.LEADER;
    }

    public State getState() {
        return state;
    }

    public Map<Short, Integer> getNextIndexMap() {
        return nextIndexMap;
    }

    public Map<Short, Integer> getMatchIndexMap() {
        return matchIndexMap;
    }

    public Map<Short, Boolean> getVotesMap() {
        return votesMap;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public void updateLeader(short leaderId) {
        if (state != State.FOLLOWER) {
            throw new IllegalStateException("Server could not set leader because state is " + state.toString() + " but should be FOLOOWER!");
        }
        this.currentLeader = leaderId;
    }

    public short getCurrentLeader() {
        return currentLeader;
    }

    public void updateVote(short votedFor) {
        if (state != State.FOLLOWER) {
            throw new IllegalStateException("Server could not set vote because state is " + state.toString() + " but should be FOLLOWER!");
        }
        this.votedFor = votedFor;
        this.resetTimer();
    }

    public short getVotedFor() {
        return votedFor;
    }

    /**
     * Changes the state to Leader. This happens when the server got a quorum of servers that voted for it.
     */
    public void convertStateToLeader() {
        if (state != State.CANDIDATE) {
            throw new IllegalStateException("Server could not convert to leader because state is " + state.toString() + " but should be CANDIDATE!");
        }

        System.out.println("Server " + context.getLocalId() + " converting to Leader in term " + currentTerm + "!");

        state = State.LEADER;

        for (short serverId : context.getRaftServers()) {
            nextIndexMap.put(serverId, log.getLastIndex() + 1);
            matchIndexMap.put(serverId, 0);
        }

        resetTimer();
    }

    public void resetStateAsLeader() {
        if (state != State.LEADER) {
            throw new IllegalStateException("Server could not be reset because state is " + state.toString() + " but should be LEADER!");
        }

        resetTimer();
    }

    /**
     * Changes the state to Follower. This happens when a message with a higher term is received.
     */
    public void convertStateToFollower() {
        if (state == State.FOLLOWER) {
            throw new IllegalStateException("Server could not convert to follower because state is " + state.toString() + " but should be CANDIDATE or LEADER!");
        }

        state = State.FOLLOWER;
        votedFor = 0;
        resetTimer();
    }

    public void resetStateAsFollower() {
        if (state != State.FOLLOWER) {
            throw new IllegalStateException("Server could not be reset because state is " + state.toString() + " but should be FOLLOWER!");
        }

        resetTimer();
    }

    /**
     * Resets the timer. The timeout duration is dependent on the current status.
     */
    private void resetTimer() {
        timer.cancel();
        startTimer();
    }

    public void startTimer() {
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
     * Changes the state to Candidate. This happens when the server times out as Follower.
     */
    public void convertStateToCandidate() {
        if (state != State.FOLLOWER) {
            throw new IllegalStateException("Server could not convert to candidate because state is " + state.toString() + " but should be FOLLOWER!");
        }

        if (RaftServer.electionDebug) {
            System.out.println("Server " + context.getLocalId() + " converting to Candidate!");
        }
        state = State.CANDIDATE;
        resetStateAsCandidate();
    }

    public void resetStateAsCandidate() {
        if (state != State.CANDIDATE) {
            throw new IllegalStateException("Server could not be reset because state is " + state.toString() + " but should be CANDIDATE!");
        }

        currentTerm++;
        currentLeader = 0;
        votesMap.clear();
        votedFor = context.getLocalId();
        resetTimer();
    }

    /**
     * Checks if the term of the message is higher than the local term. If this is the case, state is changed to follower.
     * @param term
     */
    public void checkTerm(int term) {
        if (term > currentTerm) {

            // if server receives message with higher term it has to convert to follower
            // if it already is a follower, only reset the timer and clear the current vote
            if (state != State.FOLLOWER) {
                if (RaftServer.electionDebug) {
                    System.out.println("Server " + context.getLocalId() + " converting to Follower because received message with higher term!");
                }

                convertStateToFollower();
            } else {
                resetStateAsFollower();
            }
            currentTerm = term;
            currentLeader = 0;
        }
    }

    /**
     * Checks if the term of the message is higher than the local term. If this is the case, state is changed to follower. Also updates the leader to the provided leader id.
     * @param term
     * @param leader
     */
    public void checkTerm(int term, short leader) {
        if (term > currentTerm) {

            if (RaftServer.electionDebug) {
                System.out.println("Server " + context.getLocalId() + " converting to Follower because received message with higher term!");
            }

            // if server receives message with higher term it has to convert to follower
            // if it already is a follower, only reset the timer and clear the current vote
            if (state != State.FOLLOWER) {
                convertStateToFollower();
            } else {
                resetStateAsFollower();
            }
            currentTerm = term;
            currentLeader = leader;
        }
    }
}
