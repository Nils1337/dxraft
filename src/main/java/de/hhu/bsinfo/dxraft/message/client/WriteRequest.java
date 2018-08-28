package de.hhu.bsinfo.dxraft.message.client;

import de.hhu.bsinfo.dxraft.net.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.ServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class WriteRequest extends AbstractClientRequest {

    private String m_name;
    private RaftData m_value;
    private boolean m_overwrite = true;

    private transient boolean success;

    public WriteRequest(String p_name, RaftData p_value, boolean p_overwrite) {
        m_name = p_name;
        m_value = p_value;
        m_overwrite = p_overwrite;
    }

    public WriteRequest(String p_name, RaftData p_value) {
        m_name = p_name;
        m_value = p_value;
    }

    public String getName() {
        return m_name;
    }

    public RaftData getValue() {
        return m_value;
    }

    @Override
    public void onCommit(ServerContext p_context, StateMachine p_stateMachine, ServerState p_state) {
        if (!isCommitted()) {
            RaftData data = p_stateMachine.read(m_name);
            if (m_overwrite || data == null) {
                p_stateMachine.write(m_name, m_value);
                success = true;
            } else {
                success = false;
            }
        }
        super.onCommit(p_context, p_stateMachine, p_state);
    }

    @Override
    public ClientResponse buildResponse() {
        RaftAddress address = getSenderAddress();
        if (isCommitted() && address != null) {
            return new ClientResponse(getSenderAddress(), getId(), success);
        }
        return null;
    }
}
