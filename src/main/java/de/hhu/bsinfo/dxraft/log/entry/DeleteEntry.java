package de.hhu.bsinfo.dxraft.log.entry;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.server.ServerConfig;
import de.hhu.bsinfo.dxraft.server.message.RequestResponse;
import de.hhu.bsinfo.dxraft.server.message.ResponseMessageFactory;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

import java.util.UUID;

public class DeleteEntry extends AbstractLogEntry {
    private String m_name;
    private transient RaftData m_deletedData;

    public DeleteEntry(UUID p_requestId, RaftAddress p_clientAddress, int p_term, String p_name) {
        super(p_requestId, p_clientAddress, p_term);
        m_name = p_name;
    }

    @Override
    public void onCommit(ServerConfig p_context, StateMachine p_stateMachine, ServerState p_state) {
        if (!isCommitted()) {
            m_deletedData = p_stateMachine.delete(m_name);
        }
        super.onCommit(p_context, p_stateMachine, p_state);
    }

    @Override
    public RequestResponse buildResponse(ResponseMessageFactory p_responseMessageFactory) {
        RaftAddress address = getClientAddress();
        if (isCommitted() && address != null) {
            return p_responseMessageFactory.newRequestResponse(address, getRequestId(), m_deletedData != null, m_deletedData);
        }
        return null;
    }
}
