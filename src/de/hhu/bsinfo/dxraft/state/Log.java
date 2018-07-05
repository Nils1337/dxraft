package de.hhu.bsinfo.dxraft.state;

import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Log {

    private List<LogEntry> log = new ArrayList<>();
    private StateMachine stateMachine;
    private int commitIndex = -1;

    public void append(LogEntry logEntry) {
        log.add(logEntry);
    }

    public int getSize() {
        return log.size();
    }

    public boolean isEmpty() {
        return log.isEmpty();
    }

    public int getLastIndex() {
        return log.size() - 1;
    }

    public LogEntry getLastEntry() {
        return log.get(log.size()-1);
    }

    public int getLastTerm() {
        return getLastEntry().getTerm();
    }

    public int getTermByIndex(int index) {
        LogEntry entry = log.get(index);
        return entry == null ? -1 : entry.getTerm();
    }

    public LogEntry get(int index) {
        return log.get(index);
    }

    public int indexOf(LogEntry logEntry) {
        return log.indexOf(logEntry);
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public boolean contains(LogEntry logEntry) {
        return log.contains(logEntry);
    }

    public List<LogEntry> updateCommitIndex(int newCommitIndex) {
        if (newCommitIndex < this.commitIndex) {
            throw new IllegalArgumentException("The commit index must never be decreased!");
        }

        if (newCommitIndex >= log.size()) {
            throw new IllegalArgumentException("Cannot commit index that is not logged");
        }

        // return committed entries
        List<LogEntry> committedEntries = log.subList(commitIndex + 1, newCommitIndex + 1);
        this.commitIndex = newCommitIndex;

        return committedEntries;
    }

    public List<LogEntry> getNewestEntries(int fromIndex) {
        return new ArrayList<>(log.subList(fromIndex, log.size()));
    }

    /**
     * Updates the log from prevLogIndex with the log entries in newEntries
     * @param prevLogIndex
     * @param newEntries
     * @return Removed log entries
     */
    public List<LogEntry> updateLog(int prevLogIndex, List<LogEntry> newEntries) {
        if (prevLogIndex < commitIndex) {
            throw new IllegalArgumentException("Cannot update already committed entries!");
        }

        List<LogEntry> removedEntries = new ArrayList<>();
        for (int i = 0; i < newEntries.size(); i++) {

            int currentIndex = prevLogIndex + 1 + i;
            if (currentIndex >= log.size()) {
                log.add(newEntries.get(i));
                continue;
            }

            LogEntry prevEntry = log.get(prevLogIndex + 1 + i);

            if (prevEntry != null && prevEntry.getTerm() != newEntries.get(i).getTerm()) {
                List<LogEntry> logSublist = log.subList(currentIndex, log.size());
                removedEntries.addAll(logSublist);
                logSublist.clear();
                log.add(newEntries.get(i));
                continue;
            }

            log.set(currentIndex, newEntries.get(i));

        }
        return removedEntries;
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
        return isEmpty() || lastTerm > getLastEntry().getTerm() || (lastTerm == getLastEntry().getTerm() && lastIndex >= getLastIndex());
    }

    public boolean isDiffering(int prevIndex, int prevTerm) {
        return getSize() <= prevIndex || (!log.isEmpty() && (prevIndex >= 0 && log.get(prevIndex).getTerm() != prevTerm));
    }

    public static final class LogBuilder {
        private StateMachine stateMachine = new StateMachine();

        private LogBuilder() {
        }

        public static LogBuilder aLog() {
            return new LogBuilder();
        }

        public LogBuilder withStateMachine(StateMachine stateMachine) {
            this.stateMachine = stateMachine;
            return this;
        }

        public Log build() {
            Log log = new Log();
            log.stateMachine = this.stateMachine;
            return log;
        }
    }
}
