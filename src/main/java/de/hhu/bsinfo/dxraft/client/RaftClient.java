package de.hhu.bsinfo.dxraft.client;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.data.ClusterConfigData;
import de.hhu.bsinfo.dxraft.data.ServerData;
import de.hhu.bsinfo.dxraft.data.SpecialPaths;
import de.hhu.bsinfo.dxraft.data.StringData;
import de.hhu.bsinfo.dxraft.message.*;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.client.AddServerRequest;
import de.hhu.bsinfo.dxraft.message.client.AppendToListRequest;
import de.hhu.bsinfo.dxraft.message.client.ClientRequest;
import de.hhu.bsinfo.dxraft.message.client.DeleteListRequest;
import de.hhu.bsinfo.dxraft.message.client.ReadListRequest;
import de.hhu.bsinfo.dxraft.message.client.RemoveFromListRequest;
import de.hhu.bsinfo.dxraft.message.client.WriteListRequest;
import de.hhu.bsinfo.dxraft.message.client.DeleteRequest;
import de.hhu.bsinfo.dxraft.message.client.ReadRequest;
import de.hhu.bsinfo.dxraft.message.client.RemoveServerRequest;
import de.hhu.bsinfo.dxraft.message.client.WriteRequest;
import de.hhu.bsinfo.dxraft.message.server.ClientRedirection;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.net.ClientDatagramNetworkService;
import de.hhu.bsinfo.dxraft.net.ClientNetworkService;

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
    private ClientNetworkService networkService;

    public RaftClient(RaftContext context, ClientNetworkService networkService) {
        this.context = context;
        this.networkService = networkService;
    }

    public RaftClient(RaftContext context) {
        this.context = context;
        networkService = new ClientDatagramNetworkService();
    }

    public RaftData read(String path) {
        ClientResponse response = sendRequest(new ReadRequest(path));
        if (response != null) {
            return response.getValue();
        }

        return null;
    }

    public short readShort(String path) {
        ClientResponse response = sendRequest(new ReadRequest(path));
        if (response != null) {
            try {
                return Short.parseShort(((StringData) response.getValue()).getData());
            } catch (NumberFormatException | ClassCastException e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean exists(String name) {
        ClientResponse response = sendRequest(new ReadRequest(name));
        if (response != null) {
            return response.getValue() != null;
        }

        return false;
    }

    public boolean write(String name, RaftData value, boolean overwrite) {
        ClientResponse response = sendRequest(new WriteRequest(name, value, overwrite));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean write(String name, short value, boolean overwrite) {
        ClientResponse response = sendRequest(new WriteRequest(name, new StringData(String.valueOf(value)), overwrite));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public RaftData delete(String name) {
        ClientResponse response = sendRequest(new DeleteRequest(name));
        if (response != null) {
            return response.getValue();
        }

        return null;
    }

    public List<RaftData> readList(String name) {
        ClientResponse response = sendRequest(new ReadListRequest(name));
        if (response != null) {
            return response.getListValue();
        }

        return null;
    }

    public boolean listExists(String name) {
        ClientResponse response = sendRequest(new ReadListRequest(name));
        if (response != null) {
            return response.getListValue() != null;
        }

        return false;
    }

    public boolean writeList(String name, List<RaftData> value, boolean overwrite) {
        ClientResponse response = sendRequest(new WriteListRequest(name, value, overwrite));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean addToList(String name, RaftData value, boolean createIfNotExistent) {
        ClientResponse response = sendRequest(new AppendToListRequest(name, value, createIfNotExistent));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean removeFromList(String name, RaftData value, boolean deleteIfEmpty) {
        ClientResponse response = sendRequest(new RemoveFromListRequest(name, value, deleteIfEmpty));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public List<RaftData> deleteList(String name) {
        ClientResponse response = sendRequest(new DeleteListRequest(name));
        if (response != null) {
            return response.getListValue();
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

    public RaftAddress getCurrentLeader() {
        ClientResponse response = sendRequest(new ReadRequest(SpecialPaths.LEADER_PATH));
        if (response != null && response.getValue() instanceof ServerData) {
            return ((ServerData) response.getValue()).getServer();
        }

        return null;
    }

    public List<RaftAddress> getCurrentConfig() {
        ClientResponse response = sendRequest(new ReadRequest(SpecialPaths.CLUSTER_CONFIG_PATH));
        if (response != null && response.getValue() instanceof ClusterConfigData) {
            return ((ClusterConfigData) response.getValue()).getServers();
        }

        return null;
    }

    public void shutdown() {
        networkService.close();
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
                    // cluster might be trying to elect leader
                    // wait a short time
                    try {
                        Thread.sleep(retryTimeout);
                    } catch (InterruptedException e) {
                        LOGGER.error(e);
                    }
                    serverAddress = getRandomServer();
                }
            }
        }

        LOGGER.error("Failure connecting to cluster");
        // failed to connect to a leader
        return null;
    }

    private RaftAddress getRandomServer() {
        int random = ThreadLocalRandom.current().nextInt(context.getServerCount());
        return context.getRaftServers().get(random);
    }

    public static void main(String[] args) {

        //for logging
        System.setProperty("server.id", "client");

        String serverString = System.getProperty("servers");
        if (serverString == null) {
            LOGGER.error("No server list found. Must be provided with -Dservers parameter. Exiting...");
            return;
        }

        List<RaftAddress> addresses = new ArrayList<>();
        String[] servers = serverString.split(",");
        for (String server: servers) {
            String[] address = server.split(":");

            if (address.length != 2) {
                LOGGER.error("Could not parse server list. Exiting...");
                return;
            }

            int port;

            try {
                port = Integer.parseInt(address[1]);
            } catch (NumberFormatException e) {
                LOGGER.error("Could not parse server list. Exiting...", e);
                return;
            }

            addresses.add(new RaftAddress(address[0], port));
        }

        RaftContext context = new RaftContext(addresses);

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
                    boolean result = client.write(strings[1], new StringData(strings[2]), true);
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
            } else if (in.startsWith("leader")) {
                RaftAddress result = client.getCurrentLeader();
                if (result != null) {
                    System.out.println("Current leader is: " + result);
                } else {
                    System.out.println("Could not determine current leader!");
                }
            } else if (in.startsWith("config")) {
                List<RaftAddress> result = client.getCurrentConfig();
                if (result != null) {
                    System.out.println("Current configuration is: " + result);
                } else {
                    System.out.println("Could not determine current configuration!");
                }
            } else {
                System.out.println("invalid operation!");
            }

        }
    }

}
