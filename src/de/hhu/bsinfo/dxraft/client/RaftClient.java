package de.hhu.bsinfo.dxraft.client;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.data.StringData;
import de.hhu.bsinfo.dxraft.message.*;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.test.DatagramNetworkService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class RaftClient {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final boolean debug = false;

    private static final int retryTimeout = 100;
    private static final int overallTryDuration = 10 * 1000;

    private RaftContext context;
    private ClientNetworkService networkService;

    public RaftClient(RaftContext context, ClientNetworkService networkService) {
        this.context = context;
        this.networkService = networkService;
    }

    public RaftClient(RaftContext context) {
        this.context = context;
        this.networkService = new DatagramNetworkService(context);
    }

    public RaftData read(String path) {
        ClientResponse response = sendRequest(ClientRequest.RequestType.GET, path, null);
        if (response != null) {
            return response.getValue();
        }

        return null;
    }

    public boolean write(String path, RaftData value) {
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

    private ClientResponse sendRequest(ClientRequest.RequestType requestType, String path, RaftData value) {
        // select a random server to forward request to
        int random = ThreadLocalRandom.current().nextInt(context.getServersIds().size());
        RaftAddress serverAddress = context.getRaftServers().
                get(random);

        long startTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        while (currentTime - startTime + overallTryDuration > 0) {

            LOGGER.debug("Client {} sending request to server {}", context.getLocalId(), serverAddress);

            RaftMessage response = networkService.sendRequest(new ClientRequest(serverAddress, requestType, path, value));

            if (response instanceof ClientResponse) {
                LOGGER.debug("Client {} got response!", context.getLocalId());
                return (ClientResponse) response;
            }

            ClientRedirection redirection = (ClientRedirection) response;

            LOGGER.debug("Client {} got redirection to server {}!", context.getLocalId(), redirection.getLeaderAddress());

            if (redirection.getLeaderAddress() != null) {
                serverAddress = redirection.getLeaderAddress();
            } else {
                try {
                    Thread.sleep(retryTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // select other server randomly
                random = ThreadLocalRandom.current().nextInt(context.getServersIds().size());
                serverAddress = context.getRaftServers().get(random);
            }

            currentTime = System.currentTimeMillis();
        }

        // failed to connect to a leader
        return null;
    }


    public static void main(String[] args) {
        short id = 3;

        List<RaftAddress> servers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            servers.add(new RaftAddress(new RaftID((short) i), "127.0.0.1", 5000 + i));
        }

        RaftAddress localAddress = new RaftAddress(new RaftID(id), "127.0.0.1", 6000);
        RaftContext context = new RaftContext(servers, localAddress);

        RaftClient client = new RaftClient(context);

        while (true) {
            System.out.print(">> ");
            Scanner scanner = new Scanner(System.in);
            String in = scanner.nextLine();

            if (in.startsWith("read")) {
                String[] strings = in.split(" ");
                if (strings.length > 1) {
                    RaftData result = client.read(strings[1]);
                    if (result != null) {
                        System.out.println(result.toString());
                    } else {
                        System.out.println("Could not read path!");
                    }
                } else {
                    System.out.println("invalid operation!");
                }
            } else if (in.startsWith("write")) {
                String[] strings = in.split(" ");
                if (strings.length > 2) {
                    boolean result = client.write(strings[1], new StringData(strings[2]));
                    if (result) {
                        System.out.println("Write successful!");
                    } else {
                        System.out.println("Write not successful!");
                    }
                } else {
                    System.out.println("invalid operation!");
                }
            } else if (in.startsWith("delete")) {
                String[] strings = in.split(" ");
                if (strings.length > 1) {
                    boolean result = client.delete(strings[1]);
                    if (result) {
                        System.out.println("Deletion of \"" + strings[1] + "\" was successful!");
                    } else {
                        System.out.println("Deletion failed!");
                    }
                } else {
                    System.out.println("invalid operation!");
                }
            } else {
                System.out.println("invalid operation!");
            }

        }
    }

}
