package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public class AddServerRequest extends RaftMessage implements MessageDeliverer {

    private RaftAddress newServer;

    protected AddServerRequest(RaftID receiverId, RaftAddress newServer) {
        super(receiverId);
        this.newServer = newServer;
    }

    public AddServerRequest(RaftAddress receiverAddress, RaftAddress newServer) {
        super(receiverAddress);
        this.newServer = newServer;
    }

    public RaftAddress getNewServer() {
        return newServer;
    }

    @Override
    public void deliverMessage(ServerMessageReceiver messageReceiver) {
        messageReceiver.processAddServerRequest(this);
    }
}
