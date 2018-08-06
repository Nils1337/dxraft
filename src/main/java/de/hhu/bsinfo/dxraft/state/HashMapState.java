package de.hhu.bsinfo.dxraft.state;

import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.state.StateMachine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HashMapState implements StateMachine {
    private static final Logger LOGGER = LogManager.getLogger();

    private Map<String, RaftData> state = new HashMap<>();
    private Map<String, List<RaftData>> listState = new HashMap<>();

    @Override
    public RaftData read(String name) {
        return state.get(name);
    }

    @Override
    public void write(String name, RaftData value) {
        LOGGER.trace("Writing {} to path {}", value.toString(), name);
        state.put(name, value);
    }

    @Override
    public RaftData delete(String name) {
        LOGGER.trace("Deleting path {}", name);
        return state.remove(name);
    }

    @Override
    public void writeList(String name, List<RaftData> list) {
        listState.put(name, list);
    }

    @Override
    public List<RaftData> readList(String name) {
        return listState.get(name);
    }

    @Override
    public List<RaftData> deleteList(String name) {
        return listState.remove(name);
    }
}
