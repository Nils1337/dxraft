package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;

import java.util.List;
import java.util.UUID;

public interface ResponseMessageFactory {
    RequestResponse newRequestResponse(RaftAddress p_receiverAddress, UUID p_requestId, RaftAddress p_leaderAddress);
    RequestResponse newRequestResponse(RaftAddress p_receiverAddress, UUID p_requestId, boolean p_success);
    RequestResponse newRequestResponse(RaftAddress p_receiverAddress, UUID p_requestId, boolean p_success, RaftData p_value);
}
