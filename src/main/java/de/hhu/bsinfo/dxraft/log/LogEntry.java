package de.hhu.bsinfo.dxraft.log;

import de.hhu.bsinfo.dxraft.message.client.ClientRequest;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

import java.io.Serializable;

public interface LogEntry extends Serializable {
    int getTerm();
    ClientResponse buildResponse();

    void updateClientRequest(ClientRequest request);

    default void onAppend(RaftServerContext context, StateMachine stateMachine, ServerState state) {}
    default void onCommit(RaftServerContext context, StateMachine stateMachine, ServerState state) {}
    default void onRemove(RaftServerContext context, StateMachine stateMachine) {}

    boolean isCommitted();
}
