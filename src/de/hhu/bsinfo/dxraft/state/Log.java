package de.hhu.bsinfo.dxraft.state;

import de.hhu.bsinfo.dxraft.context.RaftID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Log extends ArrayList<LogEntry> {

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

    public void updateCommitIndex(int newCommitIndex) {
        if (newCommitIndex < this.commitIndex) {
            throw new IllegalArgumentException("The commit index must never be decreased!");
        }

        if (newCommitIndex >= log.size()) {
            throw new IllegalArgumentException("Cannot commit index that is not logged");
        }

        // update state machine
        for (int i = this.commitIndex + 1; i <= newCommitIndex; i++) {
            stateMachine.applyLogEntry(log.get(i));
        }
        this.commitIndex = newCommitIndex;

    }

    public List<LogEntry> getNewestEntries(int fromIndex) {
        return new ArrayList<>(log.subList(fromIndex, log.size()));
    }

    public void updateLog(int prevLogIndex, List<LogEntry> newEntries) {
        if (prevLogIndex < commitIndex) {
            throw new IllegalArgumentException("Cannot update already committed entries!");
        }

        for (int i = 0; i < newEntries.size(); i++) {

            int currentIndex = prevLogIndex + 1 + i;
            if (currentIndex >= log.size()) {
                log.add(newEntries.get(i));
                continue;
            }

            LogEntry prevEntry = log.get(prevLogIndex + 1 + i);

            if (prevEntry != null && prevEntry.getTerm() != newEntries.get(i).getTerm()) {
                log.subList(currentIndex, log.size()).clear();
                log.add(newEntries.get(i));
                continue;
            }

            log.set(currentIndex, newEntries.get(i));

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
        return isEmpty() || lastTerm > getLastEntry().getTerm() || (lastTerm == getLastEntry().getTerm() && lastIndex >= getLastIndex());
    }

    public boolean isDiffering(int prevIndex, int prevTerm) {
        return getSize() <= prevIndex || (!log.isEmpty() && (prevIndex >= 0 && log.get(prevIndex).getTerm() != prevTerm));
    }

    /**
     * Checks the matchIndexes of the followers to find a higher index where the logs of a majority of the servers match and
     * the term is the current term of the leader and returns this index. Returns the current commit index if there is no such index.
     */
    public int getNewCommitIndex(Map<RaftID, Integer> matchIndexMap, int serverCount, int currentTerm) {
        int newCommitIndex = getCommitIndex();
        for (int i = getCommitIndex() + 1; i <= getLastIndex(); i++) {
            final int index = i;
            if (matchIndexMap.values().stream().filter(matchIndex -> matchIndex >= index).count() >= Math.ceil(serverCount/2.0) && log.get(i).getTerm() == currentTerm) {
                newCommitIndex = index;
            }
        }
        return newCommitIndex;
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
