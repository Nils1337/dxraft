package de.hhu.bsinfo.dxraft.log;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Log {

    private List<LogEntry> log = new ArrayList<>();
    private int commitIndex = 0;

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

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    public List<LogEntry> getNewestEntries(int fromIndex) {
        return log.subList(fromIndex, log.size()-1);
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
