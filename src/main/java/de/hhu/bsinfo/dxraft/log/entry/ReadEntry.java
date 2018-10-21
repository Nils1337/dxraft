package de.hhu.bsinfo.dxraft.log.entry;

import de.hhu.bsinfo.dxraft.client.message.Requests;
import de.hhu.bsinfo.dxraft.data.*;
import de.hhu.bsinfo.dxraft.server.ServerConfig;
import de.hhu.bsinfo.dxraft.server.message.RequestResponse;
import de.hhu.bsinfo.dxraft.server.message.ResponseMessageFactory;
import de.hhu.bsinfo.dxraft.state.ServerState;
import de.hhu.bsinfo.dxraft.state.StateMachine;
import de.hhu.bsinfo.dxutils.serialization.Exporter;
import de.hhu.bsinfo.dxutils.serialization.Importer;
import de.hhu.bsinfo.dxutils.serialization.ObjectSizeUtil;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
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

    @Override
    public void exportObject(Exporter p_exporter) {
        p_exporter.writeByte(Requests.READ_REQUEST);
        p_exporter.writeString(m_name);
        super.exportObject(p_exporter);
    }

    @Override
    public void importObject(Importer p_importer) {
        m_name = p_importer.readString(m_name);
        super.importObject(p_importer);
    }

    @Override
    public int sizeofObject() {
        return ObjectSizeUtil.sizeofString(m_name) + Byte.BYTES + super.sizeofObject();
    }
}
