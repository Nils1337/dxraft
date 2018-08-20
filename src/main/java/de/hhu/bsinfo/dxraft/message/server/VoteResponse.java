package de.hhu.bsinfo.dxraft.message.server;

import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public class VoteResponse extends ServerMessage {
    private boolean voteGranted;

    public VoteResponse(int receiverId, int term, boolean voteGranted) {
        super(receiverId, term);
        this.voteGranted = voteGranted;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    @Override
    public void deliverMessage(ServerMessageReceiver messageReceiver) {
        messageReceiver.processVoteResponse(this);
    }
}
