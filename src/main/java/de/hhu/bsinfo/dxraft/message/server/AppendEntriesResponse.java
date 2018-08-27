package de.hhu.bsinfo.dxraft.message.server;

import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public class AppendEntriesResponse extends AbstractServerMessage {

    private boolean m_success;
    private int m_matchIndex;

    public AppendEntriesResponse(int p_receiverId, int p_term, boolean p_success) {
        super(p_receiverId, p_term);
        m_success = p_success;
    }

    public AppendEntriesResponse(int p_receiverId, int p_term, boolean p_success, int p_matchIndex) {
        super(p_receiverId, p_term);
        m_success = p_success;
        m_matchIndex = p_matchIndex;
    }

    @Override
    public void deliverMessage(ServerMessageReceiver p_messageReceiver) {
        p_messageReceiver.processAppendEntriesResponse(this);
    }

    public boolean isSuccess() {
        return m_success;
    }

    public int getMatchIndex() {
        return m_matchIndex;
    }
}
