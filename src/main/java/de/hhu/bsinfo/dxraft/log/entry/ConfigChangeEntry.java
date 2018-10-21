package de.hhu.bsinfo.dxraft.log.entry;

import de.hhu.bsinfo.dxraft.client.message.Requests;
import de.hhu.bsinfo.dxraft.data.RaftAddress;
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
public class ConfigChangeEntry extends AbstractLogEntry {
    public static final byte ADD_SERVER_MODE = 0;
    public static final byte REMOVE_SERVER_MODE = 1;

    private RaftAddress m_serverAddress;
    private byte m_mode;
    private transient boolean m_pending = false;

    public ConfigChangeEntry(UUID p_requestId, RaftAddress p_clientAddress, int p_term,
                             RaftAddress p_serverAddress, byte p_mode) {
        super(p_requestId, p_clientAddress, p_term);
        m_serverAddress = p_serverAddress;
        m_mode = p_mode;
    }

    @Override
    public void onAppend(ServerConfig p_context, StateMachine p_stateMachine, ServerState p_state) {
        super.onAppend(p_context, p_stateMachine, p_state);
        if (!m_pending) {
            if (m_mode == ADD_SERVER_MODE) {
                p_context.startAddServer(m_serverAddress);

                if (m_serverAddress.getId() == p_context.getLocalId()) {
                    // if the server itself is added to the configuration it should actively take part in the cluster
                    p_state.becomeActive();
                }
            } else if (m_mode == REMOVE_SERVER_MODE) {
                p_context.startRemoveServer(m_serverAddress);
            }

            m_pending = true;
        }
    }

    @Override
    public void onRemove(ServerConfig p_context, StateMachine p_stateMachine) {
        if (m_pending) {
            if (m_mode == ADD_SERVER_MODE) {
                p_context.cancelAddServer(m_serverAddress);
            } else if (m_mode == REMOVE_SERVER_MODE) {
                p_context.cancelRemoveServer(m_serverAddress);
            }
            m_pending = false;
        }
    }

    @Override
    public void onCommit(ServerConfig p_context, StateMachine p_stateMachine, ServerState p_state) {
        if (!isCommitted() && m_pending) {
            if (m_mode == ADD_SERVER_MODE) {
                p_context.finishAddServer(m_serverAddress);
            } else if (m_mode == REMOVE_SERVER_MODE) {
                p_context.finishRemoveServer(m_serverAddress);

                if (m_serverAddress.getId() == p_context.getLocalId()) {
                    // if the removed server is the server itself, it should now stop taking part in the cluster
                    p_state.becomeIdle();
                }
            }
        }
        super.onCommit(p_context, p_stateMachine, p_state);
    }

    @Override
    public RequestResponse buildResponse(ResponseMessageFactory p_responseMessageFactory) {
        RaftAddress address = getClientAddress();
        if (isCommitted() && address != null) {
            return p_responseMessageFactory.newRequestResponse(address, getRequestId(), true);
        }
        return null;
    }

    @Override
    public void exportObject(Exporter p_exporter) {
        p_exporter.writeByte(Requests.CONFIG_CHANGE_REQUEST);
        p_exporter.writeByte(m_mode);
        p_exporter.exportObject(m_serverAddress);
        super.exportObject(p_exporter);
    }

    @Override
    public void importObject(Importer p_importer) {
        m_mode = p_importer.readByte(m_mode);
        m_serverAddress = new RaftAddress();
        p_importer.importObject(m_serverAddress);
        super.importObject(p_importer);
    }

    @Override
    public int sizeofObject() {
        return m_serverAddress.sizeofObject() + 2 * Byte.BYTES + super.sizeofObject();
    }
}
