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

    public RaftData delete(String path) {
        return state.remove(path);
    }
}
