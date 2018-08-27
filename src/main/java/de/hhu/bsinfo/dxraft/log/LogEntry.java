package de.hhu.bsinfo.dxraft.log;

import de.hhu.bsinfo.dxraft.message.client.AbstractClientRequest;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

import java.io.Serializable;

public interface LogEntry extends Serializable {
    int getTerm();
    ClientResponse buildResponse();

    void updateClientRequest(AbstractClientRequest p_request);

    default void onAppend(RaftServerContext p_context, StateMachine p_stateMachine, ServerState p_state) {}
    default void onCommit(RaftServerContext p_context, StateMachine p_stateMachine, ServerState p_state) {}
    default void onRemove(RaftServerContext p_context, StateMachine p_stateMachine) {}

    boolean isCommitted();
}
