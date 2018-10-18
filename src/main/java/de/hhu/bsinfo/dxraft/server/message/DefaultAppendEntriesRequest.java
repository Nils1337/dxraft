package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.log.entry.LogEntry;
import de.hhu.bsinfo.dxraft.net.dxnet.RaftMessages;
import de.hhu.bsinfo.dxraft.server.message.AppendEntriesRequest;
import de.hhu.bsinfo.dxraft.server.net.ServerMessageReceiver;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class DefaultAppendEntriesRequest implements AppendEntriesRequest, Serializable {

    public DefaultAppendEntriesRequest(short p_receiverId, int p_term, int p_prevLogIndex, int p_prevLogTerm,
                                       int p_leaderCommitIndex, List<LogEntry> p_entries) {
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
}
