package de.hhu.bsinfo.dxraft.log;

import java.util.List;

public interface LogStorage {
    void append(LogEntry logEntry);

    LogEntry getEntryByIndex(int index);

    int getSize();

    boolean isEmpty();

    boolean contains(LogEntry logEntry);

    List<LogEntry> getEntriesByRange(int fromIndex, int toIndex);

    void removeEntriesByRange(int fromIndex, int toIndex);

    int indexOf(LogEntry logEntry);
}
