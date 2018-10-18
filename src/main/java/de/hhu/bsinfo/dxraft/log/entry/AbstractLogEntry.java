package de.hhu.bsinfo.dxraft.log.entry;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.server.ServerConfig;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractLogEntry implements LogEntry {

    private int m_term;
    @EqualsAndHashCode.Include
    private UUID m_requestId;
    private RaftAddress m_clientAddress;
    private transient boolean m_isCommitted = false;

    AbstractLogEntry(final UUID p_requestId, final RaftAddress p_clientAddress, final int p_term) {
        m_term = p_term;
        m_requestId = p_requestId;
        m_clientAddress = p_clientAddress;
    }

    @Override
    public void updateClientAddress(RaftAddress p_clientAddress) {
        m_clientAddress = p_clientAddress;
    }

    @Override
    public void onCommit(ServerConfig p_context, StateMachine p_stateMachine, ServerState p_state) {
        m_isCommitted = true;
    }
}
