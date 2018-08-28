package de.hhu.bsinfo.dxraft.message.client;

import de.hhu.bsinfo.dxraft.net.RaftAddress;
import de.hhu.bsinfo.dxraft.data.ClusterConfigData;
import de.hhu.bsinfo.dxraft.data.SpecialPaths;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.ServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class RemoveServerRequest extends AbstractClientRequest {

    private RaftAddress m_oldServer;

    private transient boolean m_pending = false;

    public RemoveServerRequest(RaftAddress p_oldServer) {
        m_oldServer = p_oldServer;
    }

    public RaftAddress getOldServer() {
        return m_oldServer;
    }

    @Override
    public void onAppend(ServerContext p_context, StateMachine p_stateMachine, ServerState p_state) {
        super.onAppend(p_context, p_stateMachine, p_state);
        if (!m_pending) {
            p_context.startRemoveServer(m_oldServer);
            m_pending = true;
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
    public void onRemove(ServerContext p_context, StateMachine p_stateMachine) {
        if (m_pending) {
            p_context.cancelRemoveServer(m_oldServer);
            m_pending = false;
        }
    }

    @Override
    public void onCommit(ServerContext p_context, StateMachine p_stateMachine, ServerState p_state) {
        p_context.finishRemoveServer(m_oldServer);
        p_stateMachine.write(SpecialPaths.CLUSTER_CONFIG_PATH, new ClusterConfigData(p_context.getServers()));

        if (m_oldServer.equals(p_context.getLocalAddress())) {
            // if the removed server is the server itself, it should now stop taking part in the cluster
            p_state.becomeIdle();
        }
        super.onCommit(p_context, p_stateMachine, p_state);
    }
}
