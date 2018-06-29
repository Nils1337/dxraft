package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class AddServerRequest extends ClientRequest {

    private RaftAddress newServer;

    public AddServerRequest(RaftAddress newServer) {
        this.newServer = newServer;
    }

    public RaftAddress getNewServer() {
        return newServer;
    }

    @Override
    public void commit(StateMachine stateMachine, RaftServerContext context) {
        if (!committed) {
            context.addServer(newServer);
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