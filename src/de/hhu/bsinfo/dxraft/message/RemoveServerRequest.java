package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.data.ClusterConfigData;
import de.hhu.bsinfo.dxraft.data.SpecialPaths;
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
    public void onAppend(RaftContext context, StateMachine stateMachine) {
        super.onAppend(context, stateMachine);
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
            return new ClientResponse(getSenderAddress(), true);
        }
        return null;
    }

    @Override
    public void onRemove(RaftContext context, StateMachine stateMachine) {
        if (serverRemoved) {
            context.addServer(oldServer);
            serverRemoved = false;
        }
    }
}
