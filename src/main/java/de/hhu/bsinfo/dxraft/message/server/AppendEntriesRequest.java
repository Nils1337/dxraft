package de.hhu.bsinfo.dxraft.message.server;

import de.hhu.bsinfo.dxraft.log.LogEntry;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

import java.util.List;

public class AppendEntriesRequest extends AbstractServerMessage {

    private int m_prevLogIndex;
    private int m_prevLogTerm;
    private int m_leaderCommitIndex;
    private List<LogEntry> m_entries;

    public AppendEntriesRequest(int p_receiverId, int p_term, int p_prevLogIndex, int p_prevLogTerm,
        int p_leaderCommitIndex, List<LogEntry> p_entries) {
        super(p_receiverId, p_term);
        m_prevLogIndex = p_prevLogIndex;
        m_prevLogTerm = p_prevLogTerm;
        m_leaderCommitIndex = p_leaderCommitIndex;
        m_entries = p_entries;
    }

    @Override
    public void deliverMessage(ServerMessageReceiver p_messageReceiver) {
        p_messageReceiver.processAppendEntriesRequest(this);
    }

    public int getPrevLogIndex() {
        return m_prevLogIndex;
    }

    public int getPrevLogTerm() {
        return m_prevLogTerm;
    }

    public List<LogEntry> getEntries() {
        return m_entries;
    }

    public int getLeaderCommitIndex() {
        return m_leaderCommitIndex;
    }
}
