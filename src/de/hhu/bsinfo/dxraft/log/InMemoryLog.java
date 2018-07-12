package de.hhu.bsinfo.dxraft.log;

import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

import java.util.ArrayList;
import java.util.List;

public class InMemoryLog implements LogStorage {

    private RaftContext context;
    private StateMachine stateMachine;
    private ServerState state;
    private List<LogEntry> log = new ArrayList<>();

    public InMemoryLog(RaftContext context) {
        this.context = context;
    }

    public void setStateMachine(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    public void setState(ServerState state) {
        this.state = state;
    }

    @Override
    public void append(LogEntry logEntry) {
        logEntry.onAppend(context, stateMachine, state);
        log.add(logEntry);
    }

    @Override
    public LogEntry getEntryByIndex(int index) {
        return log.get(index);
    }

    @Override
    public int getSize() {
        return log.size();
    }

    @Override
    public boolean isEmpty() {
        return log.isEmpty();
    }

    @Override
    public boolean contains(LogEntry logEntry) {
        return log.contains(logEntry);
    }

    @Override
    public List<LogEntry> getEntriesByRange(int fromIndex, int toIndex) {
        return new ArrayList<>(log.subList(fromIndex, toIndex));
    }

    @Override
    public void removeEntriesByRange(int fromIndex, int toIndex) {
        List<LogEntry> sublist = log.subList(fromIndex, toIndex);
        sublist.forEach(entry -> entry.onRemove(context, stateMachine));
        sublist.clear();
    }

    @Override
    public int indexOf(LogEntry logEntry) {
        return log.indexOf(logEntry);
    }
}
