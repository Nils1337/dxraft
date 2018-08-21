package de.hhu.bsinfo.dxraft.message.client;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

public class CompareAndSetRequest extends ClientRequest {
    private String name;
    private RaftData value;
    private RaftData compareValue;

    private transient boolean success;

    public CompareAndSetRequest(String name, RaftData value, RaftData compareValue) {
        this.name = name;
        this.value = value;
        this.compareValue = compareValue;
    }


    public String getName() {
        return name;
    }

    public RaftData getValue() {
        return value;
    }

    public RaftData getCompareValue() {
        return compareValue;
    }

    @Override
    public void onCommit(RaftServerContext context, StateMachine stateMachine, ServerState state) {
        if (!isCommitted()) {
            RaftData data = stateMachine.read(name);
            if (data == null && compareValue == null || data != null && data.equals(compareValue)) {
                stateMachine.write(name, value);
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
