package de.hhu.bsinfo.dxraft.state;

import java.util.List;

import de.hhu.bsinfo.dxraft.data.RaftData;

public interface StateMachine {

    RaftData read(String name);

    void write(String name, RaftData data);

    RaftData delete(String name);

    void writeList(String name, List<RaftData> list);

    List<RaftData> readList(String name);

    List<RaftData> deleteList(String name);
}

