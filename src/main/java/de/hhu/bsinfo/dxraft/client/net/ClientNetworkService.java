package de.hhu.bsinfo.dxraft.client.net;

import de.hhu.bsinfo.dxraft.client.message.Request;
import de.hhu.bsinfo.dxraft.server.message.RequestResponse;

public interface ClientNetworkService {
    RequestResponse sendRequest(Request p_request);
    void close();
}
