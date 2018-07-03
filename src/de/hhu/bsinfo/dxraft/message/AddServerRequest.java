package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;

public class AddServerRequest extends ClientRequest {

    private RaftAddress newServer;

    public boolean serverAdded = false;

    public AddServerRequest(RaftAddress newServer) {
        this.newServer = newServer;
    }

    public RaftAddress getNewServer() {
        return newServer;
    }

    @Override
    public void onAppend(RaftServerContext context, ServerState state) {
        super.onAppend(context, state);
        if (!serverAdded) {
            context.addServer(newServer);
            serverAdded = true;
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
        if (serverAdded) {
            context.addServer(newServer);
            serverAdded = false;
        }
    }
}
