package de.hhu.bsinfo.dxraft.net;

import de.hhu.bsinfo.dxraft.message.RaftMessage;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public abstract class AbstractServerNetworkService {

    private ServerMessageReceiver m_messageReceiver;

    public ServerMessageReceiver getMessageReceiver() {
        return m_messageReceiver;
    }

    public abstract void sendMessage(RaftMessage p_message);

    public abstract void startReceiving();

    public abstract void stopReceiving();

    public abstract void close();

    public void setMessageReceiver(ServerMessageReceiver p_messageReceiver) {
        m_messageReceiver = p_messageReceiver;
    }


}
