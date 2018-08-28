package de.hhu.bsinfo.dxraft.log;

import de.hhu.bsinfo.dxraft.server.ServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

import java.util.ArrayList;
import java.util.List;

public class InMemoryLog implements LogStorage {

    private ServerContext m_context;
    private StateMachine m_stateMachine;
    private ServerState m_state;
    private List<LogEntry> m_log = new ArrayList<>();

    public InMemoryLog(ServerContext p_context) {
        m_context = p_context;
    }

    @Override
    public void setStateMachine(StateMachine p_stateMachine) {
        m_stateMachine = p_stateMachine;
    }

    @Override
    public void setState(ServerState p_state) {
        m_state = p_state;
    }

    @Override
    public void append(LogEntry p_logEntry) {
        p_logEntry.onAppend(m_context, m_stateMachine, m_state);
        m_log.add(p_logEntry);
    }

    @Override
    public LogEntry getEntryByIndex(int p_index) {
        return m_log.get(p_index);
    }

    @Override
    public int getSize() {
        return m_log.size();
    }

    @Override
    public boolean isEmpty() {
        return m_log.isEmpty();
    }

    @Override
    public boolean contains(LogEntry p_logEntry) {
        return m_log.contains(p_logEntry);
    }

    @Override
    public List<LogEntry> getEntriesByRange(int p_fromIndex, int p_toIndex) {
        return new ArrayList<>(m_log.subList(p_fromIndex, p_toIndex));
    }

    @Override
    public void removeEntriesByRange(int p_fromIndex, int p_toIndex) {
        List<LogEntry> sublist = m_log.subList(p_fromIndex, p_toIndex);
        sublist.forEach(entry -> entry.onRemove(m_context, m_stateMachine));
        sublist.clear();
    }

    @Override
    public int indexOf(LogEntry p_logEntry) {
        return m_log.indexOf(p_logEntry);
    }
}
