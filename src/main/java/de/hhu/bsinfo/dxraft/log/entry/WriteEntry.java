package de.hhu.bsinfo.dxraft.log.entry;

import de.hhu.bsinfo.dxraft.client.message.Requests;
import de.hhu.bsinfo.dxraft.data.DataTypes;
import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
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
public class WriteEntry extends AbstractLogEntry {
    public static final byte NO_OVERWRITE_MODE = 0;
    public static final byte OVERWRITE_MODE = 1;
    public static final byte COMPARE_AND_SET_MODE = 2;

    private String m_name;
    private RaftData m_value;
    private RaftData m_compareValue;
    private byte m_mode;
    private transient boolean m_success;

    public WriteEntry(UUID p_requestId, RaftAddress p_clientAddress, int p_term, String p_name, RaftData p_value,
                      byte p_mode) {
        super(p_requestId, p_clientAddress, p_term);
        m_name = p_name;
        m_value = p_value;
        m_mode = p_mode;
    }

    public WriteEntry(UUID p_requestId, RaftAddress p_clientAddress, int p_term, String p_name, RaftData p_value,
                      byte p_mode, RaftData p_compareValue) {
        super(p_requestId, p_clientAddress, p_term);
        m_name = p_name;
        m_value = p_value;
        m_mode = p_mode;
        m_compareValue = p_compareValue;
    }

    @Override
    public void onCommit(ServerConfig p_context, StateMachine p_stateMachine, ServerState p_state) {
        if (!isCommitted()) {
            RaftData data = p_stateMachine.read(m_name);

            if (m_mode == COMPARE_AND_SET_MODE) {
                if (data == null ? m_compareValue == null : data.equals(m_compareValue)) {
                    p_stateMachine.write(m_name, m_value);
                    m_success = true;
                } else {
                    m_success = false;
                }
            } else if (m_mode == OVERWRITE_MODE || data == null) {
                p_stateMachine.write(m_name, m_value);
                m_success = true;
            } else {
                m_success = false;
            }
        }
        super.onCommit(p_context, p_stateMachine, p_state);
    }

    @Override
    public RequestResponse buildResponse(ResponseMessageFactory p_responseMessageFactory) {
        RaftAddress address = getClientAddress();
        if (isCommitted() && address != null) {
            return p_responseMessageFactory.newRequestResponse(getClientAddress(), getRequestId(), m_success);
        }
        return null;
    }

    @Override
    public void exportObject(Exporter p_exporter) {
        p_exporter.writeByte(Requests.WRITE_REQUEST);
        p_exporter.writeString(m_name);
        p_exporter.writeByte(m_mode);
        p_exporter.exportObject(m_value);

        if (m_mode == COMPARE_AND_SET_MODE) {
            p_exporter.exportObject(m_compareValue);
        }

        super.exportObject(p_exporter);
    }

    @Override
    public void importObject(Importer p_importer) {
        m_name = p_importer.readString(m_name);
        m_mode = p_importer.readByte(m_mode);
        m_value = DataTypes.fromType(p_importer.readByte((byte) 0));
        p_importer.importObject(m_value);

        if (m_mode == COMPARE_AND_SET_MODE) {
            m_compareValue = DataTypes.fromType(p_importer.readByte((byte) 0));
            p_importer.importObject(m_compareValue);
        }

        super.importObject(p_importer);
    }

    @Override
    public int sizeofObject() {
        int size = ObjectSizeUtil.sizeofString(m_name) + 2 * Byte.BYTES + m_value.sizeofObject() + super.sizeofObject();

        if (m_mode == COMPARE_AND_SET_MODE) {
            size += m_compareValue.sizeofObject();
        }

        return size;
    }
}
