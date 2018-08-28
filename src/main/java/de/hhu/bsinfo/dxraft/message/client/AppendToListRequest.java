package de.hhu.bsinfo.dxraft.message.client;

import java.util.ArrayList;
import java.util.List;

import de.hhu.bsinfo.dxraft.net.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.ServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class AppendToListRequest extends AbstractClientRequest {
    private String m_name;
    private RaftData m_value;
    private boolean m_createIfNotExistent;

    private transient boolean m_success;

    public AppendToListRequest(String p_name, RaftData p_value, boolean p_createIfNotExistent) {
        m_name = p_name;
        m_value = p_value;
        m_createIfNotExistent = p_createIfNotExistent;
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

            if (list == null && m_createIfNotExistent) {
                List<RaftData> newList = new ArrayList<>();
                newList.add(m_value);
                p_stateMachine.writeList(m_name, newList);
                m_success = true;
            } else if (list != null) {
                list.add(m_value);
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
