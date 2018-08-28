package de.hhu.bsinfo.dxraft.message.client;

import java.util.List;

import de.hhu.bsinfo.dxraft.net.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.ServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class RemoveFromListRequest extends AbstractClientRequest {

    private String m_name;
    private RaftData m_value;
    private boolean m_deleteIfEmpty;

    private transient boolean m_success;

    public RemoveFromListRequest(String p_name, RaftData p_value, boolean p_deleteIfEmpty) {
        m_name = p_name;
        m_value = p_value;
        m_deleteIfEmpty = p_deleteIfEmpty;
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
            List<RaftData> list = p_stateMachine.readList(m_name);
            if (list != null) {
                m_success = list.remove(m_value);
                if (m_success && list.isEmpty() && m_deleteIfEmpty) {
                    p_stateMachine.deleteList(m_name);
                }
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
