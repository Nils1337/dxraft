package de.hhu.bsinfo.dxraft.net;

import de.hhu.bsinfo.dxraft.message.RaftMessage;
import de.hhu.bsinfo.dxraft.message.client.ClientRequest;

public interface ClientNetworkService {
    RaftMessage sendRequest(ClientRequest request);
    void close();
}
