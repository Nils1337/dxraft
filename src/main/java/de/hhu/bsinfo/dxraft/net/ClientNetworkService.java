package de.hhu.bsinfo.dxraft.net;

import de.hhu.bsinfo.dxraft.message.RaftMessage;
import de.hhu.bsinfo.dxraft.message.client.AbstractClientRequest;

public interface ClientNetworkService {
    RaftMessage sendRequest(AbstractClientRequest p_request);
    void close();
}
