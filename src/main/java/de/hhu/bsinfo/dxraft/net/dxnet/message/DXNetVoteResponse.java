package de.hhu.bsinfo.dxraft.net.dxnet.message;

import de.hhu.bsinfo.dxnet.core.AbstractMessageExporter;
import de.hhu.bsinfo.dxnet.core.AbstractMessageImporter;
import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.net.dxnet.RaftMessages;
import de.hhu.bsinfo.dxraft.server.message.VoteResponse;
import de.hhu.bsinfo.dxutils.serialization.ObjectSizeUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DXNetVoteResponse extends AbstractDXNetServerMessage implements VoteResponse {
    private RaftAddress m_senderAddress;
    private RaftAddress m_receiverAddress;
    private short m_senderId;
    private short m_receiverId;
    private int m_term;
    private boolean m_voteGranted;

    public DXNetVoteResponse(short p_receiverId, int p_term, boolean p_voteGranted) {
        super(p_receiverId, RaftMessages.DXRAFT_MESSAGE, RaftMessages.VOTE_RESPONSE);
        m_receiverId = p_receiverId;
        m_term = p_term;
        m_voteGranted = p_voteGranted;
    }
    @Override
    protected void writePayload(AbstractMessageExporter p_exporter) {
        p_exporter.writeShort(m_senderId);
        p_exporter.writeInt(m_term);
        p_exporter.writeBoolean(m_voteGranted);
    }

    @Override
    protected void readPayload(AbstractMessageImporter p_importer) {
        m_senderId = p_importer.readShort(m_senderId);
        m_term = p_importer.readInt(m_term);
        m_voteGranted = p_importer.readBoolean(m_voteGranted);
    }

    @Override
    protected int getPayloadLength() {
        return ObjectSizeUtil.sizeofBoolean() + Short.BYTES + Integer.BYTES;
    }
}
