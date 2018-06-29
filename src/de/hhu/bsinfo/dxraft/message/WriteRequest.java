package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
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
    public void commit(StateMachine stateMachine, RaftServerContext context) {
        if (!committed) {
            stateMachine.write(path, value);
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
