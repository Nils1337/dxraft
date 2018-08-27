package de.hhu.bsinfo.dxraft.state;

import de.hhu.bsinfo.dxraft.data.RaftData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HashMapState implements StateMachine {
    private static final Logger LOGGER = LogManager.getLogger();

    private Map<String, RaftData> m_state = new HashMap<>();
    private Map<String, List<RaftData>> m_listState = new HashMap<>();

    @Override
    public RaftData read(String p_name) {
        return m_state.get(p_name);
    }

    @Override
    public void write(String p_name, RaftData p_value) {
        LOGGER.trace("Writing {} to path {}", p_value.toString(), p_name);
        m_state.put(p_name, p_value);
    }

    @Override
    public RaftData delete(String p_name) {
        LOGGER.trace("Deleting path {}", p_name);
        return m_state.remove(p_name);
    }

    @Override
    public void writeList(String p_name, List<RaftData> p_list) {
        m_listState.put(p_name, p_list);
    }

    @Override
    public List<RaftData> readList(String p_name) {
        return m_listState.get(p_name);
    }

    @Override
    public List<RaftData> deleteList(String p_name) {
        return m_listState.remove(p_name);
    }
}
