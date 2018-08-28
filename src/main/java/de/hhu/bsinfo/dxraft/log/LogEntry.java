package de.hhu.bsinfo.dxraft.log;

import de.hhu.bsinfo.dxraft.message.client.AbstractClientRequest;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.ServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

import java.io.Serializable;

public interface LogEntry extends Serializable {
    int getTerm();
    ClientResponse buildResponse();

    void updateClientRequest(AbstractClientRequest p_request);

    default void onAppend(ServerContext p_context, StateMachine p_stateMachine, ServerState p_state) {}
    default void onCommit(ServerContext p_context, StateMachine p_stateMachine, ServerState p_state) {}
    default void onRemove(ServerContext p_context, StateMachine p_stateMachine) {}

    boolean isCommitted();
}
