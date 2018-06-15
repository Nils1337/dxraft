package de.hhu.bsinfo.dxraft.server;

import de.hhu.bsinfo.dxraft.message.RaftMessage;

public abstract class ServerNetworkService {

    protected ServerMessageReceiver messageReceiver;

    abstract public void sendMessage(RaftMessage message);

    abstract public void sendMessageToAllServers(RaftMessage message);

    abstract public void start();

    abstract public void stop();

    public void setMessageReceiver(ServerMessageReceiver messageReceiver) {
        this.messageReceiver = messageReceiver;
    }
}
