package de.hhu.bsinfo.dxraft.message.server;

import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.message.MessageDeliverer;
import de.hhu.bsinfo.dxraft.message.RaftMessage;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public abstract class ServerMessage extends RaftMessage implements MessageDeliverer {
    private int term;

    ServerMessage(RaftID receiverId, int term) {
        super(receiverId);
        this.term = term;
    }

    public int getTerm() {
        return term;
    }

}
