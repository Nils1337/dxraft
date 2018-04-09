package de.hhu.bsinfo.dxraft.client;

import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.message.ClientRedirection;
import de.hhu.bsinfo.dxraft.message.ClientRequest;
import de.hhu.bsinfo.dxraft.message.ClientResponse;
import de.hhu.bsinfo.dxraft.message.RaftClientMessage;

import java.util.concurrent.ThreadLocalRandom;

public class RaftClient {

    private static final int retryTimeout = 100;
    private static final int overallTryDuration = 10 * 1000;

    private RaftContext context;
    private ClientNetworkService networkService;

    public RaftClient(RaftContext context, ClientNetworkService networkService) {
        this.context = context;
        this.networkService = networkService;
    }

    public Object read(String path) {
        ClientResponse response = sendRequest(ClientRequest.RequestType.GET, path, null);
        if (response != null) {
            return response.getValue();
        }

        return null;
    }

    public boolean write(String path, Object value) {
        ClientResponse response = sendRequest(ClientRequest.RequestType.PUT, path, value);
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean delete(String path) {
        ClientResponse response = sendRequest(ClientRequest.RequestType.DELETE, path, null);
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    private ClientResponse sendRequest(ClientRequest.RequestType requestType, String path, Object value) {
        // select a random server to forward request to
        int random = ThreadLocalRandom.current().nextInt(context.getRaftServers().size());
        short serverId = context.getRaftServers().get(random);

        long startTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        while (currentTime - startTime + overallTryDuration > 0) {
            RaftClientMessage response = networkService.sendRequest(new ClientRequest(context.getLocalId(), serverId, requestType, path, value));
            if (response instanceof ClientResponse) {
                return (ClientResponse) response;
            }

            ClientRedirection redirection = (ClientRedirection) response;
            if (redirection.getLeaderId() != 0) {
                serverId = redirection.getLeaderId();
            } else {
                try {
                    Thread.sleep(retryTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // select other server randomly
                random = ThreadLocalRandom.current().nextInt(context.getRaftServers().size());
                serverId = context.getRaftServers().get(random);
            }

            currentTime = System.currentTimeMillis();
        }

        // failed to connect to a leader
        return null;
    }


}
