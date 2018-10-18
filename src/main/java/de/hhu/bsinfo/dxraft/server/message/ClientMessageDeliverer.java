package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.server.net.RequestReceiver;

public interface ClientMessageDeliverer {
    void deliverMessage(RequestReceiver p_messageReceiver);
}
