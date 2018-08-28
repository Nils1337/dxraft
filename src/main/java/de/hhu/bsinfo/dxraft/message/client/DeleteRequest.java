package de.hhu.bsinfo.dxraft.message.client;

import de.hhu.bsinfo.dxraft.net.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.ServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class DeleteRequest extends AbstractClientRequest {
    private String m_name;
    private RaftData m_deletedData;

    public DeleteRequest(String p_name) {
        m_name = p_name;
    }

    public String getName() {
        return m_name;
    }

    @Override
    public void onCommit(ServerContext p_context, StateMachine p_stateMachine, ServerState p_state) {
        if (!isCommitted()) {
            m_deletedData = p_stateMachine.delete(m_name);
        }
        super.onCommit(p_context, p_stateMachine, p_state);
    }

    @Override
    public ClientResponse buildResponse() {
        RaftAddress address = getSenderAddress();
        if (isCommitted() && address != null) {
            return new ClientResponse(getSenderAddress(), getId(), m_deletedData);
        }
        return null;
    }
}
