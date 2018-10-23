package de.hhu.bsinfo.dxraft.net.dxnet.message;

import de.hhu.bsinfo.dxnet.core.AbstractMessageExporter;
import de.hhu.bsinfo.dxnet.core.AbstractMessageImporter;
import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.net.dxnet.RaftMessages;
import de.hhu.bsinfo.dxraft.server.message.VoteRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DXNetVoteRequest extends AbstractDXNetServerMessage implements VoteRequest {
    private RaftAddress m_senderAddress;
    private RaftAddress m_receiverAddress;
    private short m_senderId;
    private short m_receiverId;
    private int m_term;
    private int m_lastLogIndex;
    private int m_lastLogTerm;

    public DXNetVoteRequest(short p_receiverId, int p_term, int p_lastLogIndex, int p_lastLogTerm) {
        super(p_receiverId, RaftMessages.DXRAFT_MESSAGE, RaftMessages.VOTE_REQUEST);
        m_receiverId = p_receiverId;
        m_term = p_term;
        m_lastLogIndex = p_lastLogIndex;
        m_lastLogTerm = p_lastLogTerm;
    }

    @Override
    protected void writePayload(AbstractMessageExporter p_exporter) {
        p_exporter.writeShort(m_senderId);
        p_exporter.writeInt(m_term);
        p_exporter.writeInt(m_lastLogIndex);
        p_exporter.writeInt(m_lastLogTerm);
    }

    @Override
    protected void readPayload(AbstractMessageImporter p_importer) {
        m_senderId = p_importer.readShort(m_senderId);
        m_term = p_importer.readInt(m_term);
        m_lastLogIndex = p_importer.readInt(m_lastLogIndex);
        m_lastLogTerm = p_importer.readInt(m_lastLogTerm);
    }

    @Override
    protected int getPayloadLength() {
        return Short.BYTES + 3 * Integer.BYTES;
    }
}
