package de.hhu.bsinfo.dxraft.message.client;


import de.hhu.bsinfo.dxraft.message.MessageDeliverer;
import de.hhu.bsinfo.dxraft.message.RaftMessage;
import de.hhu.bsinfo.dxraft.server.ServerContext;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;
import de.hhu.bsinfo.dxraft.log.LogEntry;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

import java.util.Objects;
import java.util.UUID;

public abstract class AbstractClientRequest extends RaftMessage implements MessageDeliverer, LogEntry {

    private UUID m_id = UUID.randomUUID();

    private int m_term = -1;
    private transient boolean m_committed = false;

    @Override
    public void deliverMessage(ServerMessageReceiver p_messageReceiver) {
        p_messageReceiver.processClientRequest(this);
    }

    public UUID getId() {
        return m_id;
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
        return m_term;
    }

    public void setTerm(int p_term) {
        m_term = p_term;
    }

    @Override
    public boolean isCommitted() {
        return m_committed;
    }

    @Override
    public void updateClientRequest(AbstractClientRequest p_request) {
        setSenderAddress(p_request.getSenderAddress());
    }

    @Override
    public void onCommit(ServerContext p_context, StateMachine p_stateMachine, ServerState p_state) {
        m_committed = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractClientRequest request = (AbstractClientRequest) o;
        return Objects.equals(m_id, request.m_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_id);
    }
}
