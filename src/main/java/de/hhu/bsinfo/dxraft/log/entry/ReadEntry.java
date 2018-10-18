package de.hhu.bsinfo.dxraft.log.entry;

import de.hhu.bsinfo.dxraft.data.*;
import de.hhu.bsinfo.dxraft.server.ServerConfig;
import de.hhu.bsinfo.dxraft.server.message.RequestResponse;
import de.hhu.bsinfo.dxraft.server.message.ResponseMessageFactory;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;

import java.util.UUID;

public class ReadEntry extends AbstractLogEntry {
    private String m_name;
    private transient RaftData m_value;

    public ReadEntry(UUID p_requestId, RaftAddress p_clientAddress, int p_term, String p_name) {
        super(p_requestId, p_clientAddress, p_term);
        m_name = p_name;
    }

    public String getName() {
        return m_name;
    }

    @Override
    public void onCommit(ServerConfig p_context, StateMachine p_stateMachine, ServerState p_state) {
        if (!isCommitted()) {
            if (m_name.equals(SpecialPaths.LEADER_PATH)) {
                m_value = p_context.getRaftAddress();
            } else {
                m_value = p_stateMachine.read(m_name);
            }
        }
        super.onCommit(p_context, p_stateMachine, p_state);
    }

    @Override
    public RequestResponse buildResponse(ResponseMessageFactory p_responseMessageFactory) {
        if (isCommitted() && getClientAddress() != null) {
            return p_responseMessageFactory.newRequestResponse(getClientAddress(), getRequestId(), m_value != null, m_value);
        }
        return null;
    }
}
