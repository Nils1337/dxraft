package de.hhu.bsinfo.dxraft.net;

import de.hhu.bsinfo.dxraft.message.client.ClientRequest;
import de.hhu.bsinfo.dxraft.message.RaftMessage;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public abstract class ServerNetworkService {

    private ServerMessageReceiver messageReceiver;

    public ServerMessageReceiver getMessageReceiver() {
        return messageReceiver;
    }

    public abstract void sendMessage(RaftMessage message);

    public abstract void startReceiving();

    public abstract void stopReceiving();

    public abstract void close();

    public void setMessageReceiver(ServerMessageReceiver messageReceiver) {
        this.messageReceiver = messageReceiver;
    }


}
