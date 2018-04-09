package de.hhu.bsinfo.dxraft.state;

import java.util.HashMap;
import java.util.Map;

public class StateMachine {

    private Map<String, Object> state = new HashMap<>();

    public Object read(String path) {
        return state.get(path);
    }

    public void write(String path, Object value) {
        state.put(path, value);
    }

    public void delete(String path) {
        state.remove(path);
    }
}
