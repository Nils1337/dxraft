package de.hhu.bsinfo.dxraft.message.client;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class DeleteRequest extends ClientRequest {
    private String name;
    private RaftData deletedData;

    public DeleteRequest(String name) {
        this.name = name;
    }

    public String getPath() {
        return name;
    }

    @Override
    public void onCommit(RaftContext context, StateMachine stateMachine, ServerState state) {
        if (!isCommitted()) {
            deletedData = stateMachine.delete(name);
        }
        super.onCommit(context, stateMachine, state);
    }

    @Override
    public ClientResponse buildResponse() {
        RaftAddress address = getSenderAddress();
        if (isCommitted() && address != null) {
            return new ClientResponse(getSenderAddress(), deletedData);
        }
        return null;
    }
}