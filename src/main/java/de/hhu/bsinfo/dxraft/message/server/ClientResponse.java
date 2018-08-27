package de.hhu.bsinfo.dxraft.message.server;


import java.util.List;
import java.util.UUID;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.RaftMessage;

public class ClientResponse extends RaftMessage {

    private boolean m_success;
    private RaftData m_value;
    private List<RaftData> m_listValue;
    private UUID m_requestId;

    public ClientResponse(RaftAddress p_receiverAddress, UUID p_requestId, boolean p_success) {
        super(p_receiverAddress);
        m_success = p_success;
        m_requestId = p_requestId;
    }

    public ClientResponse(RaftAddress p_receiverAddress, UUID p_requestId, RaftData p_value) {
        super(p_receiverAddress);
        m_value = p_value;
        m_requestId = p_requestId;
    }

    public ClientResponse(RaftAddress p_receiverAddress, UUID p_requestId, List<RaftData> p_value) {
        super(p_receiverAddress);
        m_listValue = p_value;
        m_requestId = p_requestId;
    }

    public boolean isSuccess() {
        return m_success;
    }

    public RaftData getValue() {
        return m_value;
    }

    public List<RaftData> getListValue() {
        return m_listValue;
    }

    public UUID getRequestId() {
        return m_requestId;
    }
}
