package de.hhu.bsinfo.dxraft.state;

import de.hhu.bsinfo.dxraft.data.RaftData;

public interface StateMachine {

    RaftData read(String path);

    void write(String path, RaftData value);

    RaftData delete(String path);
}

