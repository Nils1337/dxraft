package de.hhu.bsinfo.dxraft.message.client;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class CompareAndSetRequest extends AbstractClientRequest {
    private String m_name;
    private RaftData m_value;
    private RaftData m_compareValue;

    private transient boolean m_success;

    public CompareAndSetRequest(String p_name, RaftData p_value, RaftData p_compareValue) {
        m_name = p_name;
        m_value = p_value;
        m_compareValue = p_compareValue;
    }


    public String getName() {
        return m_name;
    }

    public RaftData getValue() {
        return m_value;
    }

    public RaftData getCompareValue() {
        return m_compareValue;
    }

    @Override
    public void onCommit(RaftServerContext p_context, StateMachine p_stateMachine, ServerState p_state) {
        if (!isCommitted()) {
            RaftData data = p_stateMachine.read(m_name);
            if (data == null && m_compareValue == null || data != null && data.equals(m_compareValue)) {
                p_stateMachine.write(m_name, m_value);
                m_success = true;
            } else {
                m_success = false;
            }
        }
        super.onCommit(p_context, p_stateMachine, p_state);
    }

    @Override
    public ClientResponse buildResponse() {
        RaftAddress address = getSenderAddress();
        if (isCommitted() && address != null) {
            return new ClientResponse(getSenderAddress(), getId(), m_success);
        }
        return null;
    }
}
