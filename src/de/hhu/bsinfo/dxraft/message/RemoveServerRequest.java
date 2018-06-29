package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class RemoveServerRequest extends ClientRequest {

    private RaftAddress oldServer;

    public RemoveServerRequest(RaftAddress oldServer) {
        this.oldServer = oldServer;
    }

    public RaftAddress getOldServer() {
        return oldServer;
    }

    @Override
    public void commit(StateMachine stateMachine, RaftServerContext context) {
        if (!committed) {
            context.removeServer(oldServer);
            committed = true;
        }
    }

    @Override
    public ClientResponse buildResponse() {
        if (committed) {
            return new ClientResponse(getSenderAddress(), true);
        }
        return null;
    }
}
