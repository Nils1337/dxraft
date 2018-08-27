package de.hhu.bsinfo.dxraft.message.client;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.data.ClusterConfigData;
import de.hhu.bsinfo.dxraft.data.SpecialPaths;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class AddServerRequest extends AbstractClientRequest {

    private RaftAddress m_newServer;

    private transient boolean m_serverAdded = false;

    public AddServerRequest(RaftAddress p_newServer) {
        m_newServer = p_newServer;
    }

    public RaftAddress getNewServer() {
        return m_newServer;
    }

    @Override
    public void onAppend(RaftServerContext p_context, StateMachine p_stateMachine, ServerState p_state) {
        super.onAppend(p_context, p_stateMachine, p_state);
        if (!m_serverAdded) {
            p_context.addServer(m_newServer);
            p_stateMachine.write(SpecialPaths.CLUSTER_CONFIG_PATH, new ClusterConfigData(p_context.getRaftServers()));
            m_serverAdded = true;

            if (m_newServer.equals(p_context.getLocalAddress())) {
                // if the server itself is added to the configuration it should actively take part in the cluster
                p_state.becomeActive();
            }
        }
    }

    @Override
    public ClientResponse buildResponse() {
        RaftAddress address = getSenderAddress();
        if (isCommitted() && address != null) {
            return new ClientResponse(getSenderAddress(), getId(), true);
        }
        return null;
    }

    @Override
    public void onRemove(RaftServerContext p_context, StateMachine p_stateMachine) {
        if (m_serverAdded) {
            p_context.removeServer(m_newServer);
            m_serverAdded = false;
        }
    }
}
