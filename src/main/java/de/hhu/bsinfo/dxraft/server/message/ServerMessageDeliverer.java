package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.server.net.ServerMessageReceiver;

public interface ServerMessageDeliverer {
    void deliverMessage(ServerMessageReceiver p_messageReceiver);
}
