package de.hhu.bsinfo.dxraft.log.entry;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.server.ServerConfig;
import de.hhu.bsinfo.dxraft.server.message.RequestResponse;
import de.hhu.bsinfo.dxraft.server.message.ResponseMessageFactory;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

import java.io.Serializable;

public interface LogEntry extends Serializable {
    int getTerm();
    RequestResponse buildResponse(ResponseMessageFactory p_responseFactory);

    void updateClientAddress(RaftAddress p_clientAddress);

    default void onAppend(ServerConfig p_context, StateMachine p_stateMachine, ServerState p_state) {}
    default void onCommit(ServerConfig p_context, StateMachine p_stateMachine, ServerState p_state) {}
    default void onRemove(ServerConfig p_context, StateMachine p_stateMachine) {}

    boolean isCommitted();
}
