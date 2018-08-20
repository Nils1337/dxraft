package de.hhu.bsinfo.dxraft.message.client;


import de.hhu.bsinfo.dxraft.message.MessageDeliverer;
import de.hhu.bsinfo.dxraft.message.RaftMessage;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;
import de.hhu.bsinfo.dxraft.log.LogEntry;
import de.hhu.bsinfo.dxraft.state.ServerState;
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

    public void setTerm(int term) {
        this.term = term;
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
    public void onCommit(RaftServerContext context, StateMachine stateMachine, ServerState state) {
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
