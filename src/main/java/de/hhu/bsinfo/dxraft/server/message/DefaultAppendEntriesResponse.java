package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.log.entry.LogEntry;
import de.hhu.bsinfo.dxraft.net.dxnet.RaftMessages;
import de.hhu.bsinfo.dxraft.server.message.AppendEntriesResponse;
import de.hhu.bsinfo.dxraft.server.net.ServerMessageReceiver;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Data
public class DefaultAppendEntriesResponse implements AppendEntriesResponse, Serializable {

    public DefaultAppendEntriesResponse(short p_receiverId, int p_term, boolean p_success, int p_matchIndex) {
        m_receiverId = p_receiverId;
        m_term = p_term;
        m_success = p_success;
        m_matchIndex = p_matchIndex;
    }

    private RaftAddress m_senderAddress;
    private RaftAddress m_receiverAddress;
    private short m_senderId;
    private short m_receiverId;
    private int m_term;
    private boolean m_success;
    private int m_matchIndex;
}
