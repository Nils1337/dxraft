package de.hhu.bsinfo.dxraft.message.client;

import java.util.ArrayList;
import java.util.List;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class AppendToListRequest extends ClientRequest {
    private String name;
    private RaftData value;
    private boolean createIfNotExistent;

    private transient boolean success;

    public AppendToListRequest(String name, RaftData value, boolean createIfNotExistent) {
        this.name = name;
        this.value = value;
        this.createIfNotExistent = createIfNotExistent;
    }

    public String getName() {
        return name;
    }

    public RaftData getValue() {
        return value;
    }

    @Override
    public void onCommit(RaftServerContext context, StateMachine stateMachine, ServerState state) {
        if (!isCommitted()) {
            List<RaftData> list = stateMachine.readList(name);

            if (list == null && createIfNotExistent) {
                List<RaftData> newList = new ArrayList<>();
                newList.add(value);
                stateMachine.writeList(name, newList);
                success = true;
            } else if (list != null) {
                list.add(value);
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
            return new ClientResponse(getSenderAddress(), getId(), success);
        }
        return null;
    }
}
