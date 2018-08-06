package de.hhu.bsinfo.dxraft.message.client;

import java.util.List;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class RemoveFromListRequest extends ClientRequest {

    private String name;
    private RaftData value;

    private transient boolean success;

    public RemoveFromListRequest(String name, RaftData value) {
        this.name = name;
        this.value = value;
    }

    public String getPath() {
        return name;
    }

    public RaftData getValue() {
        return value;
    }

    @Override
    public void onCommit(RaftContext context, StateMachine stateMachine, ServerState state) {
        if (!isCommitted()) {
            List<RaftData> list = stateMachine.readList(name);
            if (list != null) {
                success = list.remove(value);
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
