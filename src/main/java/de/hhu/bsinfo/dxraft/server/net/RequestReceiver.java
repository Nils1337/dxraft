package de.hhu.bsinfo.dxraft.server.net;

import de.hhu.bsinfo.dxraft.client.message.Request;

public interface RequestReceiver {
    void processClientRequest(Request p_request);
}
