package de.hhu.bsinfo.dxraft.message.server;

import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public class VoteResponse extends AbstractServerMessage {
    private boolean m_voteGranted;

    public VoteResponse(int p_receiverId, int p_term, boolean p_voteGranted) {
        super(p_receiverId, p_term);
        m_voteGranted = p_voteGranted;
    }

    public boolean isVoteGranted() {
        return m_voteGranted;
    }

    @Override
    public void deliverMessage(ServerMessageReceiver p_messageReceiver) {
        p_messageReceiver.processVoteResponse(this);
    }
}
