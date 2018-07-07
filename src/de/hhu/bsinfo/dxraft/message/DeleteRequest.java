package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class DeleteRequest extends ClientRequest {
    private String path;
    private RaftData deletedData;

    public DeleteRequest(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public void onCommit(RaftContext context, StateMachine stateMachine) {
        if (!isCommitted()) {
            deletedData = stateMachine.delete(path);
        }
        super.onCommit(context, stateMachine);
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
