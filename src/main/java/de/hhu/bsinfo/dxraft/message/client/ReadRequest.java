package de.hhu.bsinfo.dxraft.message.client;

import de.hhu.bsinfo.dxraft.net.RaftAddress;
import de.hhu.bsinfo.dxraft.data.ClusterConfigData;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.data.ServerData;
import de.hhu.bsinfo.dxraft.data.SpecialPaths;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.ServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class ReadRequest extends AbstractClientRequest {
    private String m_name;
    private RaftData m_value;

    public ReadRequest(String p_name) {
        m_name = p_name;
    }

    public String getName() {
        return m_name;
    }

    @Override
    public void onCommit(ServerContext p_context, StateMachine p_stateMachine, ServerState p_state) {
        if (!isCommitted()) {
            if (m_name.equals(SpecialPaths.LEADER_PATH)) {
                m_value = new ServerData(p_context.getLocalAddress());
            } else if (m_name.equals(SpecialPaths.CLUSTER_CONFIG_PATH)) {
                m_value = new ClusterConfigData(p_context.getServers());
            } else {
                m_value = p_stateMachine.read(m_name);
            }
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
