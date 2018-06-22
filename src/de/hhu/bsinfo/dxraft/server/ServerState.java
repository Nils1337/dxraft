package de.hhu.bsinfo.dxraft.server;

import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.state.Log;
import de.hhu.bsinfo.dxraft.timer.RaftTimer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ServerState {

    private static final Logger LOGGER = LogManager.getLogger();

    public enum State {
        FOLLOWER, CANDIDATE, LEADER
    }

    /* persistent context */
    // TODO persist
    private int currentTerm = 0;
    private RaftID votedFor;
    private Log log;

    /* volatile context */
    //private int lastApplied = 0;

    private RaftID currentLeader;
    private State state = State.FOLLOWER;
    private RaftTimer timer;
    private RaftServerContext context;

    private Map<RaftID, Integer> nextIndexMap = new HashMap<>();
    private Map<RaftID, Integer> matchIndexMap = new HashMap<>();
    private Map<RaftID, Boolean> votesMap = new HashMap<>();

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

    public Map<RaftID, Integer> getNextIndexMap() {
        return nextIndexMap;
    }

    public Map<RaftID, Integer> getMatchIndexMap() {
        return matchIndexMap;
    }

    public Map<RaftID, Boolean> getVotesMap() {
        return votesMap;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public void updateLeader(RaftID leaderId) {
        if (state != State.FOLLOWER) {
            throw new IllegalStateException("Server could not set leader because state is " + state.toString() + " but should be FOLOOWER!");
        }
        this.currentLeader = leaderId;
    }

    public RaftID getCurrentLeader() {
        return currentLeader;
    }

    public void updateVote(RaftID votedFor) {
        if (state != State.FOLLOWER) {
            throw new IllegalStateException("Server could not set vote because state is " + state.toString() + " but should be FOLLOWER!");
        }
        this.votedFor = votedFor;
        timer.reset(state);
    }

    public RaftID getVotedFor() {
        return votedFor;
    }

    /**
     * Changes the state to Leader. This happens when the server got a quorum of servers that voted for it.
     */
    public void convertStateToLeader() {
        if (state != State.CANDIDATE) {
            throw new IllegalStateException("Server could not convert to leader because state is " + state.toString() + " but should be CANDIDATE!");
        }

        LOGGER.info("Server {} converting to Leader in term {}!", context.getLocalId(), currentTerm);

        state = State.LEADER;

        for (RaftID serverId : context.getRaftServers()) {
            nextIndexMap.put(serverId, log.getLastIndex() + 1);
            matchIndexMap.put(serverId, 0);
        }

        timer.reset(state);
    }

    public void resetStateAsLeader() {
        if (state != State.LEADER) {
            throw new IllegalStateException("Server could not be reset because state is " + state.toString() + " but should be LEADER!");
        }

        timer.reset(state);
    }

    /**
     * Changes the state to Follower. This happens when a message with a higher term is received.
     */
    public void convertStateToFollower() {
        if (state == State.FOLLOWER) {
            throw new IllegalStateException("Server could not convert to follower because state is " + state.toString() + " but should be CANDIDATE or LEADER!");
        }

        state = State.FOLLOWER;
        votedFor = null;
        timer.reset(state);
    }

    public void resetStateAsFollower() {
        if (state != State.FOLLOWER) {
            throw new IllegalStateException("Server could not be reset because state is " + state.toString() + " but should be FOLLOWER!");
        }

        timer.reset(state);
    }

    /**
     * Changes the state to Candidate. This happens when the server times out as Follower.
     */
    public void convertStateToCandidate() {
        if (state != State.FOLLOWER) {
            throw new IllegalStateException("Server could not convert to candidate because state is " + state.toString() + " but should be FOLLOWER!");
        }

        LOGGER.debug("Server {} converting to converting to Candidate!", context.getLocalId());
        state = State.CANDIDATE;
        resetStateAsCandidate();
    }

    public void resetStateAsCandidate() {
        if (state != State.CANDIDATE) {
            throw new IllegalStateException("Server could not be reset because state is " + state.toString() + " but should be CANDIDATE!");
        }

        currentTerm++;
        currentLeader = null;
        votesMap.clear();
        votedFor = context.getLocalId();
        timer.reset(state);
    }

    /**
     * Checks if the term of the message is higher than the local term. If this is the case, state is changed to follower.
     * @param term
     */
    public void updateTerm(int term) {
        if (term < currentTerm) {
            throw new IllegalArgumentException("Decreasing the term must never happen!");
        }

        // if server receives message with higher term it has to convert to follower
        // if it already is a follower, only reset the timer and clear the current vote
        if (state != State.FOLLOWER) {
            LOGGER.debug("Server {} converting to Follower because received message with higher term!", context.getLocalId());
            convertStateToFollower();
        } else {
            resetStateAsFollower();
        }
        currentTerm = term;
        currentLeader = null;
        votedFor = null;
    }

    public void startTimer() {
        timer.reset(state);
    }
}
