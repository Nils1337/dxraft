package de.hhu.bsinfo.dxraft.net.dxnet.message;

import de.hhu.bsinfo.dxnet.core.AbstractMessageExporter;
import de.hhu.bsinfo.dxnet.core.AbstractMessageImporter;
import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.log.LogEntryFactory;
import de.hhu.bsinfo.dxraft.log.entry.LogEntry;
import de.hhu.bsinfo.dxraft.net.dxnet.RaftMessages;
import de.hhu.bsinfo.dxraft.server.message.AppendEntriesRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DXNetAppendEntriesRequest extends AbstractDXNetServerMessage implements AppendEntriesRequest {

    public DXNetAppendEntriesRequest(short p_receiverId, int p_term, int p_prevLogIndex, int p_prevLogTerm,
                                       int p_leaderCommitIndex, List<LogEntry> p_entries) {
        super(p_receiverId, RaftMessages.DXRAFT_MESSAGE, RaftMessages.APPEND_ENTRIES_REQUEST);
        m_receiverId = p_receiverId;
        m_term = p_term;
        m_prevLogIndex = p_prevLogIndex;
        m_prevLogTerm = p_prevLogTerm;
        m_leaderCommitIndex = p_leaderCommitIndex;
        m_entries = p_entries;
    }

    private RaftAddress m_senderAddress;
    private RaftAddress m_receiverAddress;
    private short m_senderId;
    private short m_receiverId;
    private int m_term;
    private int m_prevLogIndex;
    private int m_prevLogTerm;
    private int m_leaderCommitIndex;
    private List<LogEntry> m_entries;

    @Override
    protected void writePayload(AbstractMessageExporter p_exporter) {
        p_exporter.writeShort(m_senderId);
        p_exporter.writeInt(m_term);
        p_exporter.writeInt(m_prevLogIndex);
        p_exporter.writeInt(m_prevLogTerm);
        p_exporter.writeInt(m_leaderCommitIndex);
        p_exporter.writeInt(m_entries == null ? 0 : m_entries.size());

        if (m_entries != null) {
            for (LogEntry logEntry : m_entries) {
                p_exporter.exportObject(logEntry);
            }
        }
    }

    @Override
    protected void readPayload(AbstractMessageImporter p_importer) {
        m_senderId = p_importer.readShort(m_senderId);
        m_term = p_importer.readInt(m_term);
        m_prevLogIndex = p_importer.readInt(m_prevLogIndex);
        m_prevLogTerm = p_importer.readInt(m_prevLogTerm);
        m_leaderCommitIndex = p_importer.readInt(m_leaderCommitIndex);

        int entries = 0;
        entries = p_importer.readInt(entries);
        m_entries = new ArrayList<>(entries);

        for (int i = 0; i < entries; i++) {
            LogEntry entry = LogEntryFactory.getLogEntryFromType(p_importer.readByte((byte) 0));
            p_importer.importObject(entry);
            m_entries.add(entry);
        }

    }

    @Override
    protected int getPayloadLength() {
        int size = Short.BYTES + 5 * Integer.BYTES;
        if (m_entries != null) {
            for (LogEntry entry : m_entries) {
                size += entry.sizeofObject();
            }
        }
        return size;
    }

}
