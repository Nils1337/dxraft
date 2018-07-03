package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class RemoveServerRequest extends ClientRequest {

    private RaftAddress oldServer;

    private boolean serverRemoved = false;

    public RemoveServerRequest(RaftAddress oldServer) {
        this.oldServer = oldServer;
    }

    public RaftAddress getOldServer() {
        return oldServer;
    }

    @Override
    public void onAppend(RaftServerContext context, ServerState state) {
        super.onAppend(context, state);
        if (!serverRemoved) {
            context.removeServer(oldServer);
            serverRemoved = true;
        }
    }

    @Override
    public ClientResponse buildResponse() {
        if (isCommitted()) {
            return new ClientResponse(getSenderAddress(), true);
        }
        return null;
    }

    @Override
    public void onRemove(RaftServerContext context) {
        if (serverRemoved) {
            context.addServer(oldServer);
            serverRemoved = false;
        }
    }
}
