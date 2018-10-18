package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class DefaultRequestResponse implements RequestResponse, Serializable {
    private RaftAddress m_receiverAddress;
    private RaftAddress m_senderAddress;
    private boolean m_redirection = false;
    private RaftAddress m_leaderAddress;
    private boolean m_success;
    private RaftData m_value;
    private UUID m_requestId;

    public DefaultRequestResponse(RaftAddress p_receiverAddress, UUID p_requestId, RaftAddress p_leaderAddress) {
        m_receiverAddress = p_receiverAddress;
        m_requestId = p_requestId;
        m_leaderAddress = p_leaderAddress;
        m_redirection = true;
    }

    public DefaultRequestResponse(RaftAddress p_receiverAddress, UUID p_requestId, boolean p_success) {
        m_receiverAddress = p_receiverAddress;
        m_success = p_success;
        m_requestId = p_requestId;
    }

    public DefaultRequestResponse(RaftAddress p_receiverAddress, UUID p_requestId, boolean p_success, RaftData p_value) {
        m_receiverAddress = p_receiverAddress;
        m_value = p_value;
        m_requestId = p_requestId;
        m_success = p_success;
    }
}
