package de.hhu.bsinfo.dxraft.message.client;

import java.util.List;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class ReadListRequest extends AbstractClientRequest {
    private String m_name;
    private List<RaftData> m_value;

    public ReadListRequest(String p_name) {
        this.m_name = p_name;
    }

    public String getName() {
        return m_name;
    }

    @Override
    public void onCommit(RaftServerContext p_context, StateMachine p_stateMachine, ServerState p_state) {
        if (!isCommitted()) {
            m_value = p_stateMachine.readList(m_name);
        }
        super.onCommit(p_context, p_stateMachine, p_state);
    }

    @Override
    public ClientResponse buildResponse() {
        RaftAddress address = getSenderAddress();
        if (isCommitted() && address != null) {
            return new ClientResponse(getSenderAddress(), getId(), m_value);
        }
        return null;
    }
}