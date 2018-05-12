package de.hhu.bsinfo.dxraft.state;

import de.hhu.bsinfo.dxraft.data.RaftData;

import java.util.HashMap;
import java.util.Map;

public class StateMachine {

    private Map<String, RaftData> state = new HashMap<>();

    public RaftData read(String path) {
        return state.get(path);
    }

    public void write(String path, RaftData value) {
        state.put(path, value);
    }

    public void delete(String path) {
        state.remove(path);
    }

    public void applyLogEntry(LogEntry logEntry) {
        if (logEntry.isWriting()) {
            state.put(logEntry.getPath(), logEntry.getValue());
        } else {
            logEntry.setValue(state.remove(logEntry.getPath()));
        }
    }
}
