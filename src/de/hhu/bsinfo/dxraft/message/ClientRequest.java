package de.hhu.bsinfo.dxraft.message;


import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;
import de.hhu.bsinfo.dxraft.log.LogEntry;
import de.hhu.bsinfo.dxraft.state.StateMachine;

import java.util.Objects;
import java.util.UUID;

public abstract class ClientRequest extends RaftMessage implements MessageDeliverer, LogEntry {

    private UUID id = UUID.randomUUID();

    private int term = -1;
    private transient boolean committed = false;

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
    public void onCommit(RaftContext context, StateMachine stateMachine) {
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
