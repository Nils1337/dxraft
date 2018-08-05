package de.hhu.bsinfo.dxraft.net;

import de.hhu.bsinfo.dxraft.message.client.ClientRequest;
import de.hhu.bsinfo.dxraft.message.RaftMessage;

public class DXNetService extends AbstractNetworkService {
    @Override
    public void sendMessage(RaftMessage message) {

    }

    @Override
    public void sendMessageToAllServers(RaftMessage message) {

    }

    @Override
    public RaftMessage sendRequest(ClientRequest request) {
        return null;
    }

    @Override
    public void startReceiving() {

    }

    @Override
    public void stopReceiving() {

    }
}
