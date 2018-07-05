package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.data.ClusterConfigData;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.data.SpecialPaths;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.state.StateMachine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    public void commit(StateMachine stateMachine, RaftServerContext context) {
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
        super.commit(stateMachine, context);
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
