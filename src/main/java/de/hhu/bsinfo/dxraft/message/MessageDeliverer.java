package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public interface MessageDeliverer {
    void deliverMessage(ServerMessageReceiver p_messageReceiver);
}
