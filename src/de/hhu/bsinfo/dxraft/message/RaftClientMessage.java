package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftID;

public abstract class RaftClientMessage extends RaftMessage {

    protected RaftClientMessage(RaftID senderId, RaftID receiverId) {
        super(senderId, receiverId);
    }

}
