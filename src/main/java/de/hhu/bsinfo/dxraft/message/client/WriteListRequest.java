package de.hhu.bsinfo.dxraft.message.client;

import java.util.List;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class WriteListRequest extends ClientRequest {

    private String name;
    private List<RaftData> value;
    private boolean overwrite = true;

    private transient boolean success;

    public WriteListRequest(String name, List<RaftData> value) {
        this.name = name;
        this.value = value;
    }

    public WriteListRequest(String path, List<RaftData> value, boolean overwrite) {
        this.name = path;
        this.value = value;
        this.overwrite = overwrite;
    }

    public String getPath() {
        return name;
    }

    public List<RaftData> getValue() {
        return value;
    }

    @Override
    public void onCommit(RaftContext context, StateMachine stateMachine, ServerState state) {
        if (!isCommitted()) {
            List<RaftData> data = stateMachine.readList(name);
            if (overwrite || data == null) {
                stateMachine.writeList(name, value);
                success = true;
            } else {
                success = false;
            }
        }
        super.onCommit(context, stateMachine, state);
    }

    @Override
    public ClientResponse buildResponse() {
        RaftAddress address = getSenderAddress();
        if (isCommitted() && address != null) {
            return new ClientResponse(getSenderAddress(), success);
        }
        return null;
    }
}
