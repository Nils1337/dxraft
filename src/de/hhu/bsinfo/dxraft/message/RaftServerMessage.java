package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public abstract class RaftServerMessage extends RaftMessage implements MessageDeliverer {
    private int term;

    RaftServerMessage(short senderId, short receiverId, int term) {
        super(senderId, receiverId);
        this.term = term;
    }

    public int getTerm() {
        return term;
    }

}
