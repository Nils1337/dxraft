package de.hhu.bsinfo.dxraft.client;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.data.StringData;
import de.hhu.bsinfo.dxraft.message.*;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.client.AddServerRequest;
import de.hhu.bsinfo.dxraft.message.client.ClientRequest;
import de.hhu.bsinfo.dxraft.message.client.CreateRequest;
import de.hhu.bsinfo.dxraft.message.client.DeleteRequest;
import de.hhu.bsinfo.dxraft.message.client.ReadRequest;
import de.hhu.bsinfo.dxraft.message.client.RemoveServerRequest;
import de.hhu.bsinfo.dxraft.message.client.WriteOrCreateRequest;
import de.hhu.bsinfo.dxraft.message.client.WriteRequest;
import de.hhu.bsinfo.dxraft.message.server.ClientRedirection;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.net.DatagramNetworkService;
import de.hhu.bsinfo.dxraft.net.AbstractNetworkService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class RaftClient {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int retryTimeout = 100;
    private static final int overallTryDuration = 10 * 1000;

    private RaftContext context;
    private AbstractNetworkService networkService;

    public RaftClient(RaftContext context, AbstractNetworkService networkService) {
        this.context = context;
        this.networkService = networkService;
    }

    public RaftClient(RaftContext context) {
        this.context = context;
        this.networkService = new DatagramNetworkService(context);
    }

    public RaftData read(String path) {
        ClientResponse response = sendRequest(new ReadRequest(path));
        if (response != null) {
            return response.getValue();
        }

        return null;
    }

    public boolean create(String path, RaftData value) {
        ClientResponse response = sendRequest(new CreateRequest(path, value));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean write(String path, RaftData value) {
        ClientResponse response = sendRequest(new WriteRequest(path, value));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean writeOrCreate(String path, RaftData value) {
        ClientResponse response = sendRequest(new WriteOrCreateRequest(path, value));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public RaftData delete(String path) {
        ClientResponse response = sendRequest(new DeleteRequest(path));
        if (response != null) {
            return response.getValue();
        }

        return null;
    }

    public boolean addServer(RaftAddress server) {
        ClientResponse response = sendRequest(new AddServerRequest(server));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean removeServer(RaftAddress server) {
        ClientResponse response = sendRequest(new RemoveServerRequest(server));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }


    private ClientResponse sendRequest(ClientRequest request) {
        RaftAddress serverAddress = getRandomServer();
        long startTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        while (startTime - currentTime + overallTryDuration > 0) {

            LOGGER.debug("Sending request to server {}", serverAddress);

            request.setReceiverAddress(serverAddress);
            RaftMessage response = networkService.sendRequest(request);

            currentTime = System.currentTimeMillis();

            if (response == null) {
                LOGGER.debug("No response or other communication problem with server {}", serverAddress);
                serverAddress = getRandomServer();
                continue;
            }

            if (response instanceof ClientResponse) {
                LOGGER.debug("Received response");
                return (ClientResponse) response;
            }

            if (response instanceof ClientRedirection) {
                ClientRedirection redirection = (ClientRedirection) response;

                LOGGER.debug("Received redirection to server {}", redirection.getLeaderAddress());

                if (redirection.getLeaderAddress() != null) {
                    serverAddress = redirection.getLeaderAddress();
                } else {
                    serverAddress = getRandomServer();
                }
            }

        }

        LOGGER.error("Failure connecting to cluster", context.getLocalId());
        // failed to connect to a leader
        return null;
    }

    private RaftAddress getRandomServer() {
        int random = ThreadLocalRandom.current().nextInt(context.getServersIds().size());
        return context.getRaftServers().get(random);
    }

    public static void main(String[] args) {
        short id = 3;

        System.setProperty("serverId", "client");

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
                    RaftData result = client.delete(strings[1]);
                    if (result != null) {
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
