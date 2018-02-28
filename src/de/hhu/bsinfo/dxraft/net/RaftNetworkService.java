package de.hhu.bsinfo.dxraft.net;

import de.hhu.bsinfo.dxraft.message.RaftMessage;

public abstract class RaftNetworkService {

    RaftMessageReceiver messageReceiver;

    abstract public void sendMessage(RaftMessage message);

    abstract public void start();

    abstract public void stop();

    public void setMessageReceiver(RaftMessageReceiver messageReceiver) {
        this.messageReceiver = messageReceiver;
    }
}
