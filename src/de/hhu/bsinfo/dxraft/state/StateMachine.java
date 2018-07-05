package de.hhu.bsinfo.dxraft.state;

import de.hhu.bsinfo.dxraft.data.RaftData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class StateMachine {
    private static final Logger LOGGER = LogManager.getLogger();

    private Map<String, RaftData> state = new HashMap<>();

    public RaftData read(String path) {
        return state.get(path);
    }

    public void write(String path, RaftData value) {
        LOGGER.trace("Writing {} to path {}", value.toString(), path);
        state.put(path, value);
    }

    public RaftData delete(String path) {
        LOGGER.trace("Deleting path {}", path);
        return state.remove(path);
    }
}
