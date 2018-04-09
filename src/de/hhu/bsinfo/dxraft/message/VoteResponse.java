package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public class VoteResponse extends RaftServerMessage {
    private boolean voteGranted;

    public VoteResponse(short senderId, short receiverId, int term, boolean voteGranted) {
        super(senderId, receiverId, term);
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
