package de.hhu.bsinfo.dxraft.log.entry;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.server.ServerConfig;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;
import de.hhu.bsinfo.dxutils.serialization.Exporter;
import de.hhu.bsinfo.dxutils.serialization.Importer;
import de.hhu.bsinfo.dxutils.serialization.ObjectSizeUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
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

    @Override
    public void exportObject(Exporter p_exporter) {
        p_exporter.writeString(m_requestId.toString());
        p_exporter.writeInt(m_term);
        p_exporter.exportObject(m_clientAddress);
    }

    @Override
    public void importObject(Importer p_importer) {
        m_requestId = UUID.fromString(p_importer.readString(""));
        m_term = p_importer.readInt(m_term);
        m_clientAddress = new RaftAddress();
        // read byte signalling data is an address
        p_importer.readByte((byte) 0);
        p_importer.importObject(m_clientAddress);
    }

    @Override
    public int sizeofObject() {
        return ObjectSizeUtil.sizeofString(m_requestId.toString()) + m_clientAddress.sizeofObject() + Integer.BYTES;
    }
}
