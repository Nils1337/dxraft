package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class ReadRequest extends ClientRequest {
    private String path;
    private RaftData value;

    public ReadRequest(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public void commit(StateMachine stateMachine, RaftServerContext context) {
        if (!isCommitted()) {
            value = stateMachine.read(path);
        }
        super.commit(stateMachine, context);
    }

    @Override
    public ClientResponse buildResponse() {
        if (isCommitted()) {
            return new ClientResponse(getSenderAddress(), value);
        }
        return null;
    }
}
