package de.hhu.bsinfo.dxraft.log.entry;

import de.hhu.bsinfo.dxraft.client.message.Requests;
import de.hhu.bsinfo.dxraft.data.DataTypes;
import de.hhu.bsinfo.dxraft.data.ListData;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
public class WriteListEntry extends AbstractLogEntry {
    public static final byte NO_OVERWRITE_MODE = 0;
    public static final byte OVERWRITE_MODE = 1;
    public static final byte APPEND_TO_LIST_MODE = 2;

    private String m_name;
    private RaftData m_value;
    private byte m_mode;
    private transient boolean m_success;


    public WriteListEntry(UUID p_requestId, RaftAddress p_clientAddress, int p_term, String p_name,
                           RaftData p_value, byte p_mode) {
        super(p_requestId, p_clientAddress, p_term);
        m_name = p_name;
        m_value = p_value;
        m_mode = p_mode;
    }

    @Override
    public void onCommit(ServerConfig p_context, StateMachine p_stateMachine, ServerState p_state) {
        if (!isCommitted()) {
            List<RaftData> data = p_stateMachine.readList(m_name);
            if (m_mode == APPEND_TO_LIST_MODE) {
                if (data == null) {
                    data = new ArrayList<>();
                }
                data.add(m_value);
                p_stateMachine.writeList(m_name, data);
                m_success = true;
            } else if (m_mode == OVERWRITE_MODE || data == null) {
                List<RaftData> newList = ((ListData) m_value).getData();
                p_stateMachine.writeList(m_name, newList);
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
            return p_responseMessageFactory.newRequestResponse(address, getRequestId(), m_success);
        }
        return null;
    }

    @Override
    public void exportObject(Exporter p_exporter) {
        p_exporter.writeByte(Requests.WRITE_LIST_REQUEST);
        p_exporter.writeString(m_name);
        p_exporter.writeByte(m_mode);
        p_exporter.exportObject(m_value);
        super.exportObject(p_exporter);
    }

    @Override
    public void importObject(Importer p_importer) {
        m_name = p_importer.readString(m_name);
        m_mode = p_importer.readByte(m_mode);
        m_value = DataTypes.fromType(p_importer.readByte((byte) 0));
        p_importer.importObject(m_value);
        super.importObject(p_importer);
    }

    @Override
    public int sizeofObject() {
        return ObjectSizeUtil.sizeofString(m_name) + 2 * Byte.BYTES + m_value.sizeofObject() + super.sizeofObject();
    }
}
