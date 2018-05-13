package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftID;

public class ClientRedirection extends RaftClientMessage {
    private RaftID leaderId;

    public ClientRedirection(RaftID senderId, RaftID receiverId, RaftID leaderId) {
        super(senderId, receiverId);
        this.leaderId = leaderId;
    }

    public RaftID getLeaderId() {
        return leaderId;
    }
}
