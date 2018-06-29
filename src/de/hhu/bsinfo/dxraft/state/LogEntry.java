package de.hhu.bsinfo.dxraft.state;

import de.hhu.bsinfo.dxraft.message.ClientRequest;
import de.hhu.bsinfo.dxraft.message.ClientResponse;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;

import java.io.Serializable;

public interface LogEntry extends Serializable {


    int getTerm();
    ClientResponse buildResponse();

    void updateClientRequest(ClientRequest request);

    void onAppend(RaftServerContext context, ServerState state);
    void commit(StateMachine stateMachine, RaftServerContext context);

    boolean isCommitted();
}
