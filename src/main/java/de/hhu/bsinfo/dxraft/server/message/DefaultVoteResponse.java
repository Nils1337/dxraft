package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import lombok.Data;

import java.io.Serializable;

@Data
public class DefaultVoteResponse implements VoteResponse, Serializable {
    public DefaultVoteResponse(short p_receiverId, int p_term, boolean p_voteGranted) {
        m_receiverId = p_receiverId;
        m_term = p_term;
        m_voteGranted = p_voteGranted;
    }

    private RaftAddress m_senderAddress;
    private RaftAddress m_receiverAddress;
    private short m_senderId;
    private short m_receiverId;
    private int m_term;
    private boolean m_voteGranted;
}
