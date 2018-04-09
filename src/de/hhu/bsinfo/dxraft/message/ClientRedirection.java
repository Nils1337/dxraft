package de.hhu.bsinfo.dxraft.message;

public class ClientRedirection extends RaftClientMessage {
    private short leaderId;

    public ClientRedirection(short senderId, short receiverId, short leaderId) {
        super(senderId, receiverId);
        this.leaderId = leaderId;
    }

    public short getLeaderId() {
        return leaderId;
    }
}
