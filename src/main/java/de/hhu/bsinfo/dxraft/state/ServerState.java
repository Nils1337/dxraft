package de.hhu.bsinfo.dxraft.state;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.log.Log;
import de.hhu.bsinfo.dxraft.log.entry.ConfigChangeEntry;
import de.hhu.bsinfo.dxraft.log.entry.LogEntry;
import de.hhu.bsinfo.dxraft.server.ServerConfig;
import de.hhu.bsinfo.dxraft.timer.RaftTimer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerState {

    private static final Logger LOGGER = LogManager.getLogger();

    public enum State {
        FOLLOWER, CANDIDATE, LEADER
    }

    private State m_state = State.FOLLOWER;
    private RaftTimer m_timer;
    private ServerConfig m_context;

    public ServerState(ServerConfig p_context, RaftTimer p_timer) {
        m_context = p_context;
        m_timer = p_timer;
    }

    ////////////////
    //Shared state//
    ////////////////

    // Current term this server is in
    // TODO persist
    private int m_currentTerm = 0;

    private Log m_log;

    //////////////////
    //Follower state//
    //////////////////

    // Vote the server gave in its current term
    // TODO persist
    private int m_votedFor = RaftAddress.INVALID_ID;

    // Server that this server believes is the current leader
    private int m_currentLeader = RaftAddress.INVALID_ID;

    // in idle state, timer is not started
    // -> server still answers vote and append entries request but does not try to become leader
    private boolean m_idle = true;

    ///////////////////
    //Candidate state//
    ///////////////////

    // map for the received votes
    private Map<Integer, Boolean> m_votesMap = new HashMap<Integer, Boolean>();

    /**
     * The total amount of granted votes including the vote of the server itself
     */
    public long getVotesCount() {
        long votes = m_votesMap.values().stream().filter(value -> value).count();

        // Only count the vote for itself if the server is part of its own configuration.
        // The removal of the server from the configuration might be pending but not committed, during which the server
        // should operate normally but should not count its own vote
        if (m_context.getAllServerIds().contains(m_context.getLocalId())) {
            votes++;
        }
        return  votes;
    }

    public void updateVote(int p_id, boolean p_voteGranted) {
        if (m_state != State.CANDIDATE) {
            throw new IllegalStateException("Server could not update vote map because state is "
                + m_state + " but should be CANDIDATE!");
        }
        m_votesMap.put(p_id, p_voteGranted);
    }

    ///////////////////
    //Leader state/////
    ///////////////////

    // save pending configuration requests because they are not allowed to be handled concurrently
    private List<LogEntry> m_pendingConfigChangeRequests = new ArrayList<>();

    public void addPendingConfigChangeRequest(LogEntry p_request) {
        m_pendingConfigChangeRequests.add(p_request);
    }

    public LogEntry popPendingConfigChangeRequest() {
        if (!m_pendingConfigChangeRequests.isEmpty()) {
            return m_pendingConfigChangeRequests.remove(0);
        }
        return null;
    }

    public LogEntry getPendingConfigChangeRequest() {
        if (!m_pendingConfigChangeRequests.isEmpty()) {
            return m_pendingConfigChangeRequests.get(0);
        }
        return null;
    }

    public boolean configChangeRequestisPending() {
        return m_pendingConfigChangeRequests.size() > 1;
    }

    private void resetPendingConfigChanges() {
        m_pendingConfigChangeRequests.clear();

        // if becoming leader, check if there is an uncommitted (pending)
        // configuration change in the log and add it to the pending list
        for (LogEntry logEntry : m_log.getUncommittedEntries()) {
            if (logEntry instanceof ConfigChangeEntry) {
                m_pendingConfigChangeRequests.add(logEntry);
            }
        }
    }

    // map for the next indices to send to each server
    private Map<Short, Integer> m_nextIndexMap = new HashMap<>();

    private void resetNextIndices() {
        for (Short id: m_context.getOtherServerIds()) {
            m_nextIndexMap.put(id, m_log.getLastIndex() + 1);
        }
    }

    public void decrementNextIndex(short p_id) {
        if (m_state != State.LEADER) {
            throw new IllegalStateException("Server could not update next index because state is "
                + m_state + " but should be LEADER!");
        }
        m_nextIndexMap.computeIfPresent(p_id, (k, v) -> v > 0 ? v - 1 : v);
    }

    public void updateNextIndex(short p_id, int p_index) {
        if (m_state != State.LEADER) {
            throw new IllegalStateException("Server could not update next index because state is "
                + m_state + " but should be LEADER!");
        }
        m_nextIndexMap.put(p_id, p_index);
    }

    public int getNextIndex(short p_id) {
        Integer index = m_nextIndexMap.get(p_id);
        return index == null ? 0 : index;
    }

    // map for the indices that match with the local log
    private Map<Short, Integer> m_matchIndexMap = new HashMap<>();

    private void resetMatchIndices() {
        for (Short id: m_context.getOtherServerIds()) {
            m_nextIndexMap.put(id, 0);
        }
    }

    public void updateMatchIndex(short p_id, int p_index) {
        if (m_state != State.LEADER) {
            throw new IllegalStateException("Server could not update match index because state is "
                + m_state + " but should be LEADER!");
        }
        m_matchIndexMap.put(p_id, p_index);
    }

    public int getMatchIndex(short p_id) {
        Integer index = m_matchIndexMap.get(p_id);
        return index == null ? -1 : index;
    }


    ///////////////////
    //Other Methods////
    ///////////////////

    /**
     * Checks the matchIndexes of the followers to find a higher index where the logs of a majority of the servers match
     * and the term is the current term of the leader and returns this index.
     * @return the index or the current commit index if there is no such index.
     */
    public int getNewCommitIndex() {
        int newCommitIndex = m_log.getCommitIndex();
        for (int i = m_log.getCommitIndex() + 1; i <= m_log.getLastIndex(); i++) {
            final int index = i;
            if (m_matchIndexMap.values().stream().filter(matchIndex -> matchIndex >= index).count() + 1
                > m_context.getServerCount()/2.0
                && m_log.getTermByIndex(i) == m_currentTerm) {
                newCommitIndex = index;
            }
        }
        return newCommitIndex;
    }


    public boolean isFollower() {
        return m_state == State.FOLLOWER;
    }

    public boolean isCandidate() {
        return m_state == State.CANDIDATE;
    }

    public boolean isLeader() {
        return m_state == State.LEADER;
    }


    public State getState() {
        return m_state;
    }

    public int getCurrentTerm() {
        return m_currentTerm;
    }

    public void updateLeader(int p_leaderId) {
        if (m_state != State.FOLLOWER) {
            throw new IllegalStateException("Server could not set leader because state is "
                + m_state + " but should be FOLLOWER!");
        }
        m_currentLeader = p_leaderId;
    }

    public int getCurrentLeader() {
        return m_currentLeader;
    }

    public void updateVote(int p_votedFor) {
        if (m_state != State.FOLLOWER) {
            throw new IllegalStateException("Server could not set vote because state is "
                + m_state + " but should be FOLLOWER!");
        }
        m_votedFor = p_votedFor;

        if (!m_idle) {
            m_timer.reset(m_state);
        }
    }

    public int getVotedFor() {
        return m_votedFor;
    }


    /**
     * Changes the state to Leader. This happens when the server got a quorum of servers that voted for it.
     */
    public void convertStateToLeader() {
        if (m_state != State.CANDIDATE) {
            throw new IllegalStateException("Server could not convert to leader because state is "
                + m_state + " but should be CANDIDATE!");
        }

        LOGGER.info("Server is now leader in term {}", m_currentTerm);

        m_state = State.LEADER;

        resetNextIndices();
        resetMatchIndices();
        resetPendingConfigChanges();

        m_timer.reset(m_state);
    }

    public void resetStateAsLeader() {
        if (m_state != State.LEADER) {
            throw new IllegalStateException("Server could not be reset because state is "
                + m_state + " but should be LEADER!");
        }

        m_timer.reset(m_state);
    }

    /**
     * Changes the state to Follower. This happens when a message with a higher term is received.
     */
    public void convertStateToFollower() {
        if (m_state == State.FOLLOWER) {
            throw new IllegalStateException("Server could not convert to follower because state is "
                + m_state + " but should be CANDIDATE or LEADER!");
        }

        m_state = State.FOLLOWER;
        m_votedFor = RaftAddress.INVALID_ID;
        if (!m_idle) {
            m_timer.reset(m_state);
        }
    }

    public void resetStateAsFollower() {
        if (m_state != State.FOLLOWER) {
            throw new IllegalStateException("Server could not be reset because state is "
                + m_state + " but should be FOLLOWER!");
        }

        m_timer.reset(m_state);
    }

    /**
     * Changes the state to Candidate. This happens when the server times out as Follower.
     */
    public void convertStateToCandidate() {
        if (m_state != State.FOLLOWER) {
            throw new IllegalStateException("Server could not convert to candidate because state is "
                + m_state + " but should be FOLLOWER!");
        }

        LOGGER.info("Starting election...");
        m_state = State.CANDIDATE;
        resetStateAsCandidate();
    }

    public void resetStateAsCandidate() {
        if (m_state != State.CANDIDATE) {
            throw new IllegalStateException("Server could not be reset because state is "
                + m_state + " but should be CANDIDATE!");
        }

        m_currentTerm++;
        m_currentLeader = RaftAddress.INVALID_ID;
        m_votesMap.clear();
        m_votedFor = m_context.getLocalId();
        m_timer.reset(m_state);
    }

    public void becomeIdle() {
        m_timer.cancel();
        m_idle = true;
        if (m_state != State.FOLLOWER) {
            convertStateToFollower();
        }

        LOGGER.info("Server is now idle and can be shutdown");
    }

    public void becomeActive() {
        m_idle = false;
        m_timer.reset(m_state);

        LOGGER.info("Server is now active");
    }

    public boolean isIdle() {
        return m_idle;
    }

    /**
     * Updates the current term to a new term. Converts state to follower.
     */
    public void updateTerm(int p_term) {
        if (p_term < m_currentTerm) {
            throw new IllegalArgumentException("Decreasing the term must never happen!");
        }

        // if server receives message with higher term it has to convert to follower
        // if it already is a follower, only reset the timer and clear the current vote
        if (m_state != State.FOLLOWER) {
            LOGGER.debug("Reverting state to follower because received message with higher term {}", p_term);
            convertStateToFollower();
        } else {
            resetStateAsFollower();
        }
        m_currentTerm = p_term;
        m_currentLeader = RaftAddress.INVALID_ID;
        m_votedFor = RaftAddress.INVALID_ID;
    }

    /**
     * Setter only for unit tests
     */
    public void setState(State p_state) {
        m_state = p_state;
    }

    public void setTimer(RaftTimer p_timer) {
        m_timer = p_timer;
    }

    public void setLog(Log p_log) {
        m_log = p_log;
    }
}
