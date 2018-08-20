package de.hhu.bsinfo.dxraft.log;

import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.ArrayList;
import java.util.List;

public class Log {

    private int commitIndex = -1;
    private RaftServerContext context;
    private StateMachine stateMachine;
    private ServerState state;
    private LogStorage logStorage;

    public Log(RaftServerContext context) {
        this.context = context;
    }

    public void setStateMachine(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    public void setState(ServerState state) {
        this.state = state;
    }

    public void setLogStorage(LogStorage logStorage) {
        this.logStorage = logStorage;
    }

    public void append(LogEntry logEntry) {
        logStorage.append(logEntry);
    }

    public LogEntry getEntryByIndex(int index) {
        return logStorage.getEntryByIndex(index);
    }

    public int getSize() {
        return logStorage.getSize();
    }

    public boolean isEmpty() {
        return logStorage.isEmpty();
    }

    public boolean contains(LogEntry logEntry) {
        return logStorage.contains(logEntry);
    }

    public int indexOf(LogEntry logEntry){
        return logStorage.indexOf(logEntry);
    }

    public int getLastIndex() {
        return logStorage.getSize() - 1;
    }

    public LogEntry getLastEntry() {
        return logStorage.getEntryByIndex(getLastIndex());
    }

    public int getLastTerm() {
        return getLastEntry().getTerm();
    }

    public int getTermByIndex(int index) {
        LogEntry entry = logStorage.getEntryByIndex(index);
        return entry == null ? -1 : entry.getTerm();
    }

    public List<LogEntry> getNewestEntries(int fromIndex) {
        return logStorage.getEntriesByRange(fromIndex, logStorage.getSize());
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public List<LogEntry> getUncommittedEntries() {
        if (logStorage.getSize() > commitIndex + 2) {
            return logStorage.getEntriesByRange(commitIndex + 1, logStorage.getSize());
        }
        return new ArrayList<>();
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public List<LogEntry> commitEntries(int newCommitIndex) {
        if (newCommitIndex < commitIndex) {
            throw new IllegalArgumentException("The commit index must never be decreased!");
        }

        if (newCommitIndex >= logStorage.getSize()) {
            throw new IllegalArgumentException("Cannot commit entries that do not exist");
        }

        int oldCommitIndex = commitIndex;
        commitIndex = newCommitIndex;

        // return committed entries
        List<LogEntry> committedEntries = logStorage.getEntriesByRange(oldCommitIndex + 1, newCommitIndex + 1);
        committedEntries.forEach(entry -> entry.onCommit(context, stateMachine, state));
        return committedEntries;
    }




    /**
     * Updates the log from fromIndex with the log entries in entries
     * @param fromIndex
     * @param entries
     * @return Removed log entries
     */
    public void updateEntries(int fromIndex, List<LogEntry> entries) {
        if (fromIndex < 0) {
            throw new IllegalArgumentException("fromIndex must not be smaller than zero");
        }

        for (int i = 0; i < entries.size(); i++) {
            int currentIndex = fromIndex + i;
            if (currentIndex >= logStorage.getSize()) {
                logStorage.append(entries.get(i));
                continue;
            }

            LogEntry prevEntry = logStorage.getEntryByIndex(currentIndex);

            if (prevEntry != null && prevEntry.getTerm() != entries.get(i).getTerm()) {
                if (prevEntry.isCommitted()) {
                    throw new RuntimeException("Something went wrong, server is about to overwrite a committed log entry at index " + currentIndex + "!");
                }

                logStorage.removeEntriesByRange(currentIndex, logStorage.getSize());
                logStorage.append(entries.get(i));
            }
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
    public boolean isAtLeastAsUpToDateAs(int lastTerm, int lastIndex) {
        return logStorage.isEmpty() || lastTerm > getLastTerm() || (lastTerm == getLastTerm() && lastIndex >= getLastIndex());
    }

    public boolean isDiffering(int prevIndex, int prevTerm) {
        return logStorage.getSize() <= prevIndex || (!logStorage.isEmpty() && (prevIndex >= 0 && getTermByIndex(prevIndex) != prevTerm));
    }

}
