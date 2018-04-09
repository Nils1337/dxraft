package de.hhu.bsinfo.dxraft.client;

import de.hhu.bsinfo.dxraft.message.ClientRequest;
import de.hhu.bsinfo.dxraft.message.ClientResponse;
import de.hhu.bsinfo.dxraft.message.RaftClientMessage;

public interface ClientNetworkService {
    RaftClientMessage sendRequest(ClientRequest request);
}
