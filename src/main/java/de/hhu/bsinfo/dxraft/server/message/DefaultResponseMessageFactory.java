package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;

import java.util.List;
import java.util.UUID;

public class DefaultResponseMessageFactory implements ResponseMessageFactory {
    @Override
    public RequestResponse newRequestResponse(RaftAddress p_receiverAddress, UUID p_requestId, RaftAddress p_leaderAddress) {
        return new DefaultRequestResponse(p_receiverAddress, p_requestId, p_leaderAddress);
    }

    @Override
    public RequestResponse newRequestResponse(RaftAddress p_receiverAddress, UUID p_requestId, boolean p_success) {
        return new DefaultRequestResponse(p_receiverAddress, p_requestId, p_success);
    }

    @Override
    public RequestResponse newRequestResponse(RaftAddress p_receiverAddress, UUID p_requestId, boolean p_success, RaftData p_value) {
        return new DefaultRequestResponse(p_receiverAddress, p_requestId, p_success, p_value);
    }
}
