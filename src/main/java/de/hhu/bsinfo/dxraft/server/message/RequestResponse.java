package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;

import java.util.List;
import java.util.UUID;

public interface RequestResponse {
    RaftAddress getReceiverAddress();
    void setSenderAddress(RaftAddress p_raftAddress);
    boolean isRedirection();
    RaftAddress getLeaderAddress();
    boolean isSuccess();
    RaftData getValue();
    UUID getRequestId();
}
