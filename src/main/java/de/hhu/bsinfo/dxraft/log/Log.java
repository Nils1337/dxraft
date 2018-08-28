package de.hhu.bsinfo.dxraft.log;

import de.hhu.bsinfo.dxraft.server.ServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

import java.util.ArrayList;
import java.util.List;

public class Log {

    private int m_commitIndex = -1;
    private ServerContext m_context;
    private StateMachine m_stateMachine;
    private ServerState m_state;
    private LogStorage m_logStorage;

    public Log(ServerContext p_context) {
        m_context = p_context;
    }

    public void setStateMachine(StateMachine p_stateMachine) {
        m_stateMachine = p_stateMachine;
    }

    public void setState(ServerState p_state) {
        m_state = p_state;
    }

    public void setLogStorage(LogStorage p_logStorage) {
        m_logStorage = p_logStorage;
    }

    public void append(LogEntry p_logEntry) {
        m_logStorage.append(p_logEntry);
    }

    public LogEntry getEntryByIndex(int p_index) {
        return m_logStorage.getEntryByIndex(p_index);
    }

    public int getSize() {
        return m_logStorage.getSize();
    }

    public boolean isEmpty() {
        return m_logStorage.isEmpty();
    }

    public boolean contains(LogEntry p_logEntry) {
        return m_logStorage.contains(p_logEntry);
    }

    public int indexOf(LogEntry p_logEntry){
        return m_logStorage.indexOf(p_logEntry);
    }

    public int getLastIndex() {
        return m_logStorage.getSize() - 1;
    }

    public LogEntry getLastEntry() {
        return m_logStorage.getEntryByIndex(getLastIndex());
    }

    public int getLastTerm() {
        return getLastEntry().getTerm();
    }

    public int getTermByIndex(int p_index) {
        LogEntry entry = m_logStorage.getEntryByIndex(p_index);
        return entry == null ? -1 : entry.getTerm();
    }

    public List<LogEntry> getNewestEntries(int p_fromIndex) {
        return m_logStorage.getEntriesByRange(p_fromIndex, m_logStorage.getSize());
    }

    public int getCommitIndex() {
        return m_commitIndex;
    }

    public List<LogEntry> getUncommittedEntries() {
        if (m_logStorage.getSize() > m_commitIndex + 2) {
            return m_logStorage.getEntriesByRange(m_commitIndex + 1, m_logStorage.getSize());
        }
        return new ArrayList<>();
    }

    public StateMachine getStateMachine() {
        return m_stateMachine;
    }

    public List<LogEntry> commitEntries(int p_newCommitIndex) {
        if (p_newCommitIndex < m_commitIndex) {
            throw new IllegalArgumentException("The commit index must never be decreased!");
        }

        if (p_newCommitIndex >= m_logStorage.getSize()) {
            throw new IllegalArgumentException("Cannot commit entries that do not exist");
        }

        int oldCommitIndex = m_commitIndex;
        m_commitIndex = p_newCommitIndex;

        // return committed entries
        List<LogEntry> committedEntries = m_logStorage.getEntriesByRange(oldCommitIndex + 1, p_newCommitIndex + 1);
        committedEntries.forEach(entry -> entry.onCommit(m_context, m_stateMachine, m_state));
        return committedEntries;
    }




    /**
     * Updates the log from fromIndex with the log entries in entries
     */
    public void updateEntries(int p_fromIndex, List<LogEntry> p_entries) {
        if (p_fromIndex < 0) {
            throw new IllegalArgumentException("fromIndex must not be smaller than zero");
        }

        for (int i = 0; i < p_entries.size(); i++) {
            int currentIndex = p_fromIndex + i;
            if (currentIndex >= m_logStorage.getSize()) {
                m_logStorage.append(p_entries.get(i));
                continue;
            }

            LogEntry prevEntry = m_logStorage.getEntryByIndex(currentIndex);

            if (prevEntry != null && prevEntry.getTerm() != p_entries.get(i).getTerm()) {
                if (prevEntry.isCommitted()) {
                    throw new RuntimeException("Something went wrong, server is about to overwrite a committed " +
                        "log entry at index " + currentIndex + '!');
                }

                m_logStorage.removeEntriesByRange(currentIndex, m_logStorage.getSize());
                m_logStorage.append(p_entries.get(i));
            }
        }
    }

    /**
     * Checks if other state is at least as up to date as the local state.
     * This is the case if the term of the last state entry of the other state is
     * higher than the last state entry of the local state or if
     * the term is the same and the other state is at least as long as the local state
     * @param p_lastTerm term of the last state entry of the other state
     * @param p_lastIndex last index of the other state
     * @return true if the other state is at least as up to date as the local state
     */
    public boolean isAtLeastAsUpToDateAs(int p_lastTerm, int p_lastIndex) {
        return m_logStorage.isEmpty() || p_lastTerm > getLastTerm()
            || p_lastTerm == getLastTerm() && p_lastIndex >= getLastIndex();
    }

    public boolean isDiffering(int p_prevIndex, int p_prevTerm) {
        return m_logStorage.getSize() <= p_prevIndex
            || !m_logStorage.isEmpty() && p_prevIndex >= 0 && getTermByIndex(p_prevIndex) != p_prevTerm;
    }

}
