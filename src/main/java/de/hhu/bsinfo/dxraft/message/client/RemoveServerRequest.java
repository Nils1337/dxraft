package de.hhu.bsinfo.dxraft.message.client;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.data.ClusterConfigData;
import de.hhu.bsinfo.dxraft.data.SpecialPaths;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class RemoveServerRequest extends ClientRequest {

    private RaftAddress oldServer;

    private transient boolean serverRemoved = false;

    public RemoveServerRequest(RaftAddress oldServer) {
        this.oldServer = oldServer;
    }

    public RaftAddress getOldServer() {
        return oldServer;
    }

    @Override
    public void onAppend(RaftServerContext context, StateMachine stateMachine, ServerState state) {
        super.onAppend(context, stateMachine, state);
        if (!serverRemoved) {
            context.removeServer(oldServer);
            stateMachine.write(SpecialPaths.CLUSTER_CONFIG_PATH, new ClusterConfigData(context.getRaftServers()));
            serverRemoved = true;
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
    public void onRemove(RaftServerContext context, StateMachine stateMachine) {
        if (serverRemoved) {
            context.addServer(oldServer);
            serverRemoved = false;
        }
    }

    @Override
    public void onCommit(RaftServerContext context, StateMachine stateMachine, ServerState state) {
        if (oldServer.equals(context.getLocalAddress())) {
            // if the removed server is the server itself, it should now stop taking part in the cluster
            state.becomeIdle();
        }
        super.onCommit(context, stateMachine, state);
    }
}
