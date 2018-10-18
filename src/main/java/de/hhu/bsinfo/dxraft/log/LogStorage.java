package de.hhu.bsinfo.dxraft.log;

import de.hhu.bsinfo.dxraft.log.entry.LogEntry;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

import java.util.List;

public interface LogStorage {
    void append(LogEntry p_logEntry);

    LogEntry getEntryByIndex(int p_index);

    int getSize();

    boolean isEmpty();

    boolean contains(LogEntry p_logEntry);

    List<LogEntry> getEntriesByRange(int p_fromIndex, int p_toIndex);

    void removeEntriesByRange(int p_fromIndex, int p_toIndex);

    int indexOf(LogEntry p_logEntry);

    void setStateMachine(StateMachine p_stateMachine);

    void setState(ServerState p_state);
}
