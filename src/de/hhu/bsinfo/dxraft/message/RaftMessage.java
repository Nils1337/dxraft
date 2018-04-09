package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public class RaftMessage {
    protected short senderId;
    protected short receiverId;

    protected RaftMessage(short senderId, short receiverId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    public short getSenderId() {
        return senderId;
    }

    public short getReceiverId() {
        return receiverId;
    }

}
