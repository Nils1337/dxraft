package de.hhu.bsinfo.dxraft.message.client;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.data.ClusterConfigData;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.data.ServerData;
import de.hhu.bsinfo.dxraft.data.SpecialPaths;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class ReadRequest extends ClientRequest {
    private String name;
    private RaftData value;

    public ReadRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void onCommit(RaftContext context, StateMachine stateMachine, ServerState state) {
        if (!isCommitted()) {
            if (name.equals(SpecialPaths.LEADER_PATH)) {
                value = new ServerData(context.getLocalAddress());
            } else if (name.equals(SpecialPaths.CLUSTER_CONFIG_PATH)) {
                value = new ClusterConfigData(context.getRaftServers());
            } else {
                value = stateMachine.read(name);
            }
        }
        super.onCommit(context, stateMachine, state);
    }

    @Override
    public ClientResponse buildResponse() {
        RaftAddress address = getSenderAddress();
        if (isCommitted() && address != null) {
            return new ClientResponse(getSenderAddress(), getId(), value);
        }
        return null;
    }
}
