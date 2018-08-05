package de.hhu.bsinfo.dxraft.net;

import de.hhu.bsinfo.dxraft.message.client.ClientRequest;
import de.hhu.bsinfo.dxraft.message.RaftMessage;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public abstract class AbstractNetworkService {

    private ServerMessageReceiver messageReceiver;

    public ServerMessageReceiver getMessageReceiver() {
        return messageReceiver;
    }

    abstract public void sendMessage(RaftMessage message);

    abstract public void sendMessageToAllServers(RaftMessage message);

    abstract public RaftMessage sendRequest(ClientRequest request);

    abstract public void startReceiving();

    abstract public void stopReceiving();

    public void setMessageReceiver(ServerMessageReceiver messageReceiver) {
        this.messageReceiver = messageReceiver;
    }


}
