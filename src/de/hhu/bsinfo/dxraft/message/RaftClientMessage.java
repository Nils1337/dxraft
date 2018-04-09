package de.hhu.bsinfo.dxraft.message;

public abstract class RaftClientMessage extends RaftMessage {

    protected RaftClientMessage(short senderId, short receiverId) {
        super(senderId, receiverId);
    }

}
