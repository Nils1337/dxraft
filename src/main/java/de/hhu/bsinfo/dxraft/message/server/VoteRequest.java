package de.hhu.bsinfo.dxraft.message.server;

import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public class VoteRequest extends AbstractServerMessage {
    private int m_lastLogIndex;

    public VoteRequest(int p_receiverId, int p_term, int p_lastLogIndex, int p_lastLogTerm) {
        super(p_receiverId, p_term);
        m_lastLogIndex = p_lastLogIndex;
        m_lastLogTerm = p_lastLogTerm;
    }

    private int m_lastLogTerm;

    public int getLastLogIndex() {
        return m_lastLogIndex;
    }

    public int getLastLogTerm() {
        return m_lastLogTerm;
    }

    @Override
    public void deliverMessage(ServerMessageReceiver p_messageReceiver) {
        p_messageReceiver.processVoteRequest(this);
    }
}
