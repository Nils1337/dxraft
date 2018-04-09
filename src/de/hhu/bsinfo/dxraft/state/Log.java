package de.hhu.bsinfo.dxraft.state;

import java.util.ArrayList;
import java.util.List;

public class Log {

    private List<LogEntry> log = new ArrayList<>();
    private StateMachine stateMachine = new StateMachine();
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

    public LogEntry get(int index) {
        return log.get(index);
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        if (commitIndex < this.commitIndex) {
            throw new IllegalArgumentException("The commit index must never be decreased!");
        } else {

            // TODO update state machine
            this.commitIndex = commitIndex;
        }
    }

    public List<LogEntry> getNewestEntries(int fromIndex) {
        return new ArrayList<>(log.subList(fromIndex, log.size()-1));
    }

    public void updateLog(int prevLogIndex, List<LogEntry> newEntries) {
        for (int i = 0; i < newEntries.size(); i++) {

            int currentIndex = prevLogIndex + 1 + i;
            if (currentIndex >= log.size()) {
                log.add(newEntries.get(i));
                continue;
            }

            LogEntry prevEntry = log.get(prevLogIndex + 1 + i);

            if (prevEntry != null && prevEntry.getTerm() != newEntries.get(i).getTerm()) {
                log.subList(currentIndex, log.size()-1).clear();
                log.add(newEntries.get(i));
                continue;
                //deleteAfter(prevLogIndex + i);
            }

            log.set(currentIndex, newEntries.get(i));

        }
    }
}
