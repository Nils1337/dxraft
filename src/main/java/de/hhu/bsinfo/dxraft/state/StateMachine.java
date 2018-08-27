package de.hhu.bsinfo.dxraft.state;

import java.util.List;

import de.hhu.bsinfo.dxraft.data.RaftData;

public interface StateMachine {

    RaftData read(String p_name);

    void write(String p_name, RaftData p_data);

    RaftData delete(String p_name);

    void writeList(String p_name, List<RaftData> p_list);

    List<RaftData> readList(String p_name);

    List<RaftData> deleteList(String p_name);
}

