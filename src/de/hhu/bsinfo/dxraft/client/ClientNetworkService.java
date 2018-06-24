package de.hhu.bsinfo.dxraft.client;

import de.hhu.bsinfo.dxraft.message.ClientRequest;
import de.hhu.bsinfo.dxraft.message.RaftMessage;

public interface ClientNetworkService {
    RaftMessage sendRequest(ClientRequest request);
}
