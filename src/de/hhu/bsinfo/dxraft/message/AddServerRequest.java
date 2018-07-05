package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.data.ClusterConfigData;
import de.hhu.bsinfo.dxraft.data.SpecialPaths;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class AddServerRequest extends ClientRequest {

    private RaftAddress newServer;

    public transient boolean serverAdded = false;

    public AddServerRequest(RaftAddress newServer) {
        this.newServer = newServer;
    }

    public RaftAddress getNewServer() {
        return newServer;
    }

    @Override
    public void onAppend(RaftServerContext context, ServerState state, StateMachine stateMachine) {
        super.onAppend(context, state, stateMachine);
        if (!serverAdded) {
            context.addServer(newServer);
            stateMachine.write(SpecialPaths.CLUSTER_CONFIG_PATH, new ClusterConfigData(context.getRaftServers()));
            serverAdded = true;
        }
    }

    @Override
    public ClientResponse buildResponse() {
        RaftAddress address = getSenderAddress();
        if (isCommitted() && address != null) {
            return new ClientResponse(getSenderAddress(), true);
        }
        return null;
    }

    @Override
    public void onRemove(RaftServerContext context) {
        if (serverAdded) {
            context.addServer(newServer);
            serverAdded = false;
        }
    }
}
