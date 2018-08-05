package de.hhu.bsinfo.dxraft.message.client;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.data.ClusterConfigData;
import de.hhu.bsinfo.dxraft.data.SpecialPaths;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class AddServerRequest extends ClientRequest {

    private RaftAddress newServer;

    private transient boolean serverAdded = false;

    public AddServerRequest(RaftAddress newServer) {
        this.newServer = newServer;
    }

    public RaftAddress getNewServer() {
        return newServer;
    }

    @Override
    public void onAppend(RaftContext context, StateMachine stateMachine, ServerState state) {
        super.onAppend(context, stateMachine, state);
        if (!serverAdded) {
            context.addServer(newServer);
            stateMachine.write(SpecialPaths.CLUSTER_CONFIG_PATH, new ClusterConfigData(context.getRaftServers()));
            serverAdded = true;

            if (newServer.equals(context.getLocalAddress())) {
                // if the server itself is added to the configuration it should actively take part in the cluster
                state.becomeActive();
            }
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
        if (serverAdded) {
            context.addServer(newServer);
            serverAdded = false;
        }
    }
}
