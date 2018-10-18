package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import lombok.Data;

import java.io.Serializable;

@Data
public class DefaultVoteRequest implements VoteRequest, Serializable {

    public DefaultVoteRequest(short p_receiverId, int p_term, int p_lastLogIndex, int p_lastLogTerm) {
        m_receiverId = p_receiverId;
        m_term = p_term;
        m_lastLogIndex = p_lastLogIndex;
        m_lastLogTerm = p_lastLogTerm;
    }

    private RaftAddress m_senderAddress;
    private RaftAddress m_receiverAddress;
    private short m_senderId;
    private short m_receiverId;
    private int m_term;
    private int m_lastLogIndex;
    private int m_lastLogTerm;
}
