package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class WriteRequest extends ClientRequest {

    private String path;
    private RaftData value;

    public WriteRequest(String path, RaftData value) {
        this.path = path;
        this.value = value;
    }

    public String getPath() {
        return path;
    }

    public RaftData getValue() {
        return value;
    }

    @Override
    public void onCommit(RaftContext context, StateMachine stateMachine, ServerState state) {
        if (!isCommitted()) {

            // TODO move this check to somewhere else?
//            if (path.equals(SpecialPaths.CLUSTER_CONFIG_PATH)) {
//                try {
//                    ClusterConfigData data = (ClusterConfigData) value;
//                    context.getRaftServers().clear();
//                    context.getRaftServers().addAll(data.getServers());
//                } catch (ClassCastException e) {
//                    throw new IllegalArgumentException("Path \""+ SpecialPaths.CLUSTER_CONFIG_PATH + "\" is reserved for cluster configuration!");
//                }
//            }

            stateMachine.write(path, value);
        }
        super.onCommit(context, stateMachine, state);
    }

    @Override
    public ClientResponse buildResponse() {
        RaftAddress address = getSenderAddress();
        if (isCommitted() && address != null) {
            return new ClientResponse(getSenderAddress(), true);
        }
        return null;
    }
}
