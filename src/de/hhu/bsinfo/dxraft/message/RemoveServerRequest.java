package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public class RemoveServerRequest extends RaftMessage implements MessageDeliverer {

    private RaftAddress oldServer;

    public RemoveServerRequest(RaftAddress receiverAddress, RaftAddress oldServer) {
        super(receiverAddress);
        this.oldServer = oldServer;
    }

    public RemoveServerRequest(RaftID receiverId, RaftAddress oldServer) {
        super(receiverId);
        this.oldServer = oldServer;
    }

    public RaftAddress getOldServer() {
        return oldServer;
    }

    @Override
    public void deliverMessage(ServerMessageReceiver messageReceiver) {
        messageReceiver.processRemoveServerRequest(this);
    }
}
