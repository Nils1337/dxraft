package de.hhu.bsinfo.dxraft.state;

import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
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

    private State state = State.FOLLOWER;
    private RaftTimer timer;
    private RaftServerContext context;

    public ServerState(RaftServerContext context, RaftTimer timer, Log log) {
        this.context = context;
        this.timer = timer;
        this.log = log;
    }

    ////////////////
    //Shared state//
    ////////////////

    // Current term this server is in
    // TODO persist
    private int currentTerm = 0;

    private Log log;

    //////////////////
    //Follower state//
    //////////////////

    // Vote the server gave in its current term
    // TODO persist
    private RaftID votedFor;

    // Server that this server believes is the current leader
    private RaftID currentLeader;

    ///////////////////
    //Candidate state//
    ///////////////////

    // map for the received votes
    private Map<RaftID, Boolean> votesMap = new HashMap<>();

    /**
     * The total amount of granted votes including the vote of the server itself
     */
    public long getVotesCount() {
        return votesMap.values().stream().filter((value) -> value).count() + 1;
    }

    public void updateVote(RaftID id, boolean voteGranted) {
        if (state != State.CANDIDATE) {
            throw new IllegalStateException("Server could not update vote map because state is " + state.toString() + " but should be CANDIDATE!");
        }
        votesMap.put(id, voteGranted);
    }

    ///////////////////
    //Leader state//
    ///////////////////

    // map for the next indices to send to each server
    private Map<RaftID, Integer> nextIndexMap = new HashMap<>();

    private void resetNextIndices() {
        for (RaftID id: context.getOtherServerIds()) {
            nextIndexMap.put(id, log.getLastIndex() + 1);
        }
    }

    public void decrementNextIndex(RaftID id) {
        if (state != State.LEADER) {
            throw new IllegalStateException("Server could not update next index because state is " + state.toString() + " but should be LEADER!");
        }
        nextIndexMap.computeIfPresent(id, (k, v) -> v > 0 ? v - 1 : v);
    }

    public void updateNextIndex(RaftID id, int index) {
        if (state != State.LEADER) {
            throw new IllegalStateException("Server could not update next index because state is " + state.toString() + " but should be LEADER!");
        }
        nextIndexMap.put(id, index);
    }

    public int getNextIndex(RaftID id) {
        Integer index = nextIndexMap.get(id);
        return index == null ? 0 : index;
    }

    // map for the indices that match with the local log
    private Map<RaftID, Integer> matchIndexMap = new HashMap<>();

    private void resetMatchIndices() {
        for (RaftID id: context.getOtherServerIds()) {
            nextIndexMap.put(id, 0);
        }
    }

    public void updateMatchIndex(RaftID id, int index) {
        if (state != State.LEADER) {
            throw new IllegalStateException("Server could not update match index because state is " + state.toString() + " but should be LEADER!");
        }
        matchIndexMap.put(id, index);
    }

    public int getMatchIndex(RaftID id) {
        Integer index = matchIndexMap.get(id);
        return index == null ? -1 : index;
    }

    /**
     * Checks the matchIndexes of the followers to find a higher index where the logs of a majority of the servers match and
     * the term is the current term of the leader and returns this index. Returns the current commit index if there is no such index.
     */
    public int getNewCommitIndex() {
        int newCommitIndex = log.getCommitIndex();
        for (int i = log.getCommitIndex() + 1; i <= log.getLastIndex(); i++) {
            final int index = i;
            if (matchIndexMap.values().stream().filter(matchIndex -> matchIndex >= index).count() + 1 >= Math.ceil(context.getServerCount()/2.0) && log.getTermByIndex(i) == currentTerm) {
                newCommitIndex = index;
            }
        }
        return newCommitIndex;
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

    public int getCurrentTerm() {
        return currentTerm;
    }

    public void updateLeader(RaftID leaderId) {
        if (state != State.FOLLOWER) {
            throw new IllegalStateException("Server could not set leader because state is " + state.toString() + " but should be FOLLOWER!");
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

        resetNextIndices();
        resetMatchIndices();

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

        LOGGER.debug("Server {} converting to Candidate!", context.getLocalId());
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

    /**
     * Setter only for unit tests
     */
    public void setState(State state) {
        this.state = state;
    }
}
