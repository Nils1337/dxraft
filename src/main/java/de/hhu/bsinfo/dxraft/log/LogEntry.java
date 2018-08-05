package de.hhu.bsinfo.dxraft.log;

import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.message.client.ClientRequest;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

import java.io.Serializable;

public interface LogEntry extends Serializable {
    int getTerm();
    ClientResponse buildResponse();

    void updateClientRequest(ClientRequest request);

    default void onAppend(RaftContext context, StateMachine stateMachine, ServerState state) {}
    default void onCommit(RaftContext context, StateMachine stateMachine, ServerState state) {}
    default void onRemove(RaftContext context, StateMachine stateMachine) {}

    boolean isCommitted();
}
