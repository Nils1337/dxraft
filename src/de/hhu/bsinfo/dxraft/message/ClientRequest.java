package de.hhu.bsinfo.dxraft.message;


import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;
import de.hhu.bsinfo.dxraft.state.LogEntry;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

import java.util.Objects;
import java.util.UUID;

public abstract class ClientRequest extends RaftMessage implements MessageDeliverer, LogEntry {

    private UUID id = UUID.randomUUID();

    private int term = -1;
    private boolean committed = false;

    @Override
    public void deliverMessage(ServerMessageReceiver messageReceiver) {
        messageReceiver.processClientRequest(this);
    }

    public UUID getId() {
        return id;
    }


    public boolean isReadRequest() {
        return this instanceof ReadRequest;
    }

    public boolean isConfigChangeRequest() {
        return isAddServerRequest() || isRemoveServerRequest();
    }

    public boolean isAddServerRequest() {
        return this instanceof AddServerRequest;
    }

    public boolean isRemoveServerRequest() {
        return this instanceof RemoveServerRequest;
    }

    @Override
    public int getTerm() {
        return term;
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void updateClientRequest(ClientRequest request) {
        setSenderAddress(request.getSenderAddress());
    }

    @Override
    public void onAppend(RaftServerContext context, ServerState state) {
        // Set term when this request is initially appended to log after request got to leader.
        // Later this request will be appended to the followers logs, then the term should not be updated.
        if (term == -1) {
            term = state.getCurrentTerm();
        }
    }

    @Override
    public void commit(StateMachine stateMachine, RaftServerContext context) {
        committed = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientRequest request = (ClientRequest) o;
        return Objects.equals(id, request.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
