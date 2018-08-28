package de.hhu.bsinfo.dxraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hhu.bsinfo.dxraft.net.RaftAddress;
import de.hhu.bsinfo.dxraft.data.ClusterConfigData;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.data.ServerData;
import de.hhu.bsinfo.dxraft.data.SpecialPaths;
import de.hhu.bsinfo.dxraft.data.StringData;
import de.hhu.bsinfo.dxraft.message.RaftMessage;
import de.hhu.bsinfo.dxraft.message.client.AbstractClientRequest;
import de.hhu.bsinfo.dxraft.message.client.AddServerRequest;
import de.hhu.bsinfo.dxraft.message.client.AppendToListRequest;
import de.hhu.bsinfo.dxraft.message.client.CompareAndSetRequest;
import de.hhu.bsinfo.dxraft.message.client.DeleteListRequest;
import de.hhu.bsinfo.dxraft.message.client.DeleteRequest;
import de.hhu.bsinfo.dxraft.message.client.ReadListRequest;
import de.hhu.bsinfo.dxraft.message.client.ReadRequest;
import de.hhu.bsinfo.dxraft.message.client.RemoveFromListRequest;
import de.hhu.bsinfo.dxraft.message.client.RemoveServerRequest;
import de.hhu.bsinfo.dxraft.message.client.WriteListRequest;
import de.hhu.bsinfo.dxraft.message.client.WriteRequest;
import de.hhu.bsinfo.dxraft.message.server.ClientRedirection;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;
import de.hhu.bsinfo.dxraft.net.ClientDatagramNetworkService;
import de.hhu.bsinfo.dxraft.net.ClientNetworkService;

public class RaftClient {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int RETRY_TIMEOUT = 100;
    private static final int OVERALL_TRY_DURATION = 10 * 1000;

    private ClientContext m_context;
    private ClientNetworkService m_networkService;

    public RaftClient(ClientContext p_context, ClientNetworkService p_networkService) {
        m_context = p_context;
        m_networkService = p_networkService;
    }

    public RaftClient(ClientContext p_context) {
        m_context = p_context;
        m_networkService = new ClientDatagramNetworkService();
    }

    public RaftData read(String p_path) {
        ClientResponse response = sendRequest(new ReadRequest(p_path));
        if (response != null) {
            return response.getValue();
        }

        return null;
    }

    public short readShort(String p_path) {
        ClientResponse response = sendRequest(new ReadRequest(p_path));
        if (response != null) {
            try {
                return Short.parseShort(((StringData) response.getValue()).getData());
            } catch (NumberFormatException | ClassCastException e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean exists(String p_name) {
        ClientResponse response = sendRequest(new ReadRequest(p_name));
        if (response != null) {
            return response.getValue() != null;
        }

        return false;
    }

    public boolean write(String p_name, RaftData p_value, boolean p_overwrite) {
        ClientResponse response = sendRequest(new WriteRequest(p_name, p_value, p_overwrite));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean write(String p_name, short p_value, boolean p_overwrite) {
        ClientResponse response = sendRequest(
            new WriteRequest(p_name, new StringData(String.valueOf(p_value)), p_overwrite));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean compareAndSet(String p_name, RaftData p_value, RaftData p_compareValue) {
        ClientResponse response = sendRequest(new CompareAndSetRequest(p_name, p_value, p_compareValue));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public RaftData delete(String p_name) {
        ClientResponse response = sendRequest(new DeleteRequest(p_name));
        if (response != null) {
            return response.getValue();
        }

        return null;
    }

    public List<RaftData> readList(String p_name) {
        ClientResponse response = sendRequest(new ReadListRequest(p_name));
        if (response != null) {
            return response.getListValue();
        }

        return null;
    }

    public boolean listExists(String p_name) {
        ClientResponse response = sendRequest(new ReadListRequest(p_name));
        if (response != null) {
            return response.getListValue() != null;
        }

        return false;
    }

    public boolean writeList(String p_name, List<RaftData> p_value, boolean p_overwrite) {
        ClientResponse response = sendRequest(new WriteListRequest(p_name, p_value, p_overwrite));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean addToList(String p_name, RaftData p_value, boolean p_createIfNotExistent) {
        ClientResponse response = sendRequest(new AppendToListRequest(p_name, p_value, p_createIfNotExistent));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean removeFromList(String p_name, RaftData p_value, boolean p_deleteIfEmpty) {
        ClientResponse response = sendRequest(new RemoveFromListRequest(p_name, p_value, p_deleteIfEmpty));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public List<RaftData> deleteList(String p_name) {
        ClientResponse response = sendRequest(new DeleteListRequest(p_name));
        if (response != null) {
            return response.getListValue();
        }

        return null;
    }

    public boolean addServer(RaftAddress p_server) {
        ClientResponse response = sendRequest(new AddServerRequest(p_server));
        if (response != null) {
            if (response.isSuccess()) {
                m_context.getRaftServers().add(p_server);
            }
            return response.isSuccess();
        }

        return false;
    }

    public boolean removeServer(RaftAddress p_server) {
        ClientResponse response = sendRequest(new RemoveServerRequest(p_server));
        if (response != null) {
            if (response.isSuccess()) {
                m_context.getRaftServers().remove(p_server);
            }
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
        m_networkService.close();
    }

    private ClientResponse sendRequest(AbstractClientRequest p_request) {
        RaftAddress serverAddress = getRandomServer();
        long startTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        while (startTime - currentTime + OVERALL_TRY_DURATION > 0) {

            LOGGER.debug("Sending request to server {}", serverAddress);

            p_request.setReceiverAddress(serverAddress);
            RaftMessage response = m_networkService.sendRequest(p_request);

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
                        Thread.sleep(RETRY_TIMEOUT);
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
        int random = ThreadLocalRandom.current().nextInt(m_context.getServerCount());
        return m_context.getRaftServers().get(random);
    }

    public static void main(String[] p_args) {

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

        ClientContext context = new ClientContext(addresses);

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
                        System.out.println(result);
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
            } else if (in.startsWith("add")) {
                String[] strings = in.split(" ");
                if (strings.length > 3) {
                    int id = Integer.parseInt(strings[1]);
                    String ip = strings[2];
                    int port = Integer.parseInt(strings[3]);
                    System.out.println("Adding server with id " + id + " running at " + ip + ':' + port + "...");
                    RaftAddress address = new RaftAddress(id, ip, port);
                    boolean success = client.addServer(address);
                    if (success) {
                        System.out.println("Server successfully added");
                    } else {
                        System.out.println("Adding server failed!");
                    }
                } else {
                    System.out.println("invalid operation!");
                }
            } else if (in.startsWith("remove")) {
                String[] strings = in.split(" ");
                if (strings.length > 3) {
                    int id = Integer.parseInt(strings[1]);
                    String ip = strings[2];
                    int port = Integer.parseInt(strings[3]);
                    System.out.println("Removing server with id " + id + " running at " + ip + ':' + port + "...");
                    RaftAddress address = new RaftAddress(id, ip, port);
                    boolean success = client.removeServer(address);
                    if (success) {
                        System.out.println("Server successfully removed");
                    } else {
                        System.out.println("Removing server failed!");
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
