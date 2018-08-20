package de.hhu.bsinfo.dxraft.message.server;

import de.hhu.bsinfo.dxraft.message.MessageDeliverer;
import de.hhu.bsinfo.dxraft.message.RaftMessage;

public abstract class ServerMessage extends RaftMessage implements MessageDeliverer {
    private int term;

    ServerMessage(int receiverId, int term) {
        super(receiverId);
        this.term = term;
    }

    public int getTerm() {
        return term;
    }

}
