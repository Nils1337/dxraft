package de.hhu.bsinfo.dxraft.message.client;

import java.util.List;

import de.hhu.bsinfo.dxraft.net.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.ServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class WriteListRequest extends AbstractClientRequest {

    private String m_name;
    private List<RaftData> m_value;
    private boolean m_overwrite = true;

    private transient boolean m_success;

    public WriteListRequest(String p_name, List<RaftData> p_value) {
        m_name = p_name;
        m_value = p_value;
    }

    public WriteListRequest(String p_path, List<RaftData> p_value, boolean p_overwrite) {
        m_name = p_path;
        m_value = p_value;
        m_overwrite = p_overwrite;
    }

    public String getName() {
        return m_name;
    }

    public List<RaftData> getValue() {
        return m_value;
    }

    @Override
    public void onCommit(ServerContext p_context, StateMachine p_stateMachine, ServerState p_state) {
        if (!isCommitted()) {
            List<RaftData> data = p_stateMachine.readList(m_name);
            if (m_overwrite || data == null) {
                p_stateMachine.writeList(m_name, m_value);
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
