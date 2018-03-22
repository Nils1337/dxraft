package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.net.RaftMessageReceiver;

public class VoteResponse extends RaftMessage {
    private boolean voteGranted;

    public VoteResponse(int term, short senderId, short receiverId, boolean voteGranted) {
        super(term, senderId, receiverId);
        this.voteGranted = voteGranted;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    @Override
    public void processMessage(RaftMessageReceiver messageReceiver) {
        messageReceiver.processVoteResponse(this);
    }
}
