package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.RaftServer;
import de.hhu.bsinfo.dxraft.net.RaftMessageReceiver;

public abstract class RaftMessage {
    protected int term;
    protected short senderId;
    protected short receiverId;

    protected RaftMessage(int term, short senderId, short receiverId) {
        this.term = term;
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    public int getTerm() {
        return term;
    }

    public short getSenderId() {
        return senderId;
    }

    public short getReceiverId() {
        return receiverId;
    }

    public abstract void processMessage(RaftMessageReceiver messageReceiver);
}
