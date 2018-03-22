package de.hhu.bsinfo.dxraft.log;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Log {

    List<LogEntry> log = new ArrayList<>();

    public void append(LogEntry entry) {
        log.add(entry);
    }

    public void deleteAfter(int index) {
        log = log.stream().limit(index).collect(Collectors.toList());
    }

    public int getSize() {
        return log.size();
    }

    public LogEntry get(int index) {
        return log.get(index);
    }

    public void put(int index, LogEntry entry) {
        log.add(index, entry);
    }
}
