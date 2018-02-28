package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.RaftServer;
import de.hhu.bsinfo.dxraft.net.RaftMessageReceiver;

public class VoteRequest extends RaftMessage {
    private int lastLogIndex;

    public VoteRequest(int term, short senderId, short receiverId, int lastLogIndex, int lastLogTerm) {
        super(term, senderId, receiverId);
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    private int lastLogTerm;

    public int getLastLogIndex() {
        return lastLogIndex;
    }

    public int getLastLogTerm() {
        return lastLogTerm;
    }

    @Override
    public void processMessage(RaftMessageReceiver messageReceiver) {
        messageReceiver.processVoteRequest(this);
    }
}
