package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public class RaftMessage {
    protected RaftID senderId;
    protected RaftID receiverId;

    protected RaftMessage(RaftID senderId, RaftID receiverId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    public RaftID getSenderId() {
        return senderId;
    }

    public RaftID getReceiverId() {
        return receiverId;
    }

}
