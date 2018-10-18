package de.hhu.bsinfo.dxraft.client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import de.hhu.bsinfo.dxraft.ConfigUtils;
import de.hhu.bsinfo.dxraft.DXRaft;
import de.hhu.bsinfo.dxraft.client.message.DefaultRequestFactory;
import de.hhu.bsinfo.dxraft.client.message.Request;
import de.hhu.bsinfo.dxraft.client.message.RequestFactory;
import de.hhu.bsinfo.dxraft.data.*;
import de.hhu.bsinfo.dxraft.server.ServerConfig;
import de.hhu.bsinfo.dxraft.server.message.RequestResponse;
import de.hhu.bsinfo.dxutils.JsonUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hhu.bsinfo.dxraft.net.datagram.DatagramClientNetworkService;
import de.hhu.bsinfo.dxraft.client.net.ClientNetworkService;

public class RaftClient {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CLIENT_CONFIG_PATH = "config/client-config.json";

    private ClientConfig m_context;
    private ClientNetworkService m_networkService;
    private RequestFactory m_requestFactory;

    public RaftClient(ClientConfig p_context, ClientNetworkService p_networkService, RequestFactory p_requestFactory) {
        m_context = p_context;
        m_networkService = p_networkService;
        m_requestFactory = p_requestFactory;
    }

    public RaftClient(ClientConfig p_context) {
        m_context = p_context;
        m_networkService = new DatagramClientNetworkService();
        m_requestFactory = new DefaultRequestFactory();
    }

    public RaftData read(String p_path) {
        RequestResponse response = sendRequest(m_requestFactory.getReadRequest(p_path));
        if (response != null && response.isSuccess()) {
            return response.getValue();
        }

        return null;
    }

    public short readShort(String p_path) {
        RequestResponse response = sendRequest(m_requestFactory.getReadRequest(p_path));
        if (response != null && response.isSuccess()) {
            try {
                return Short.parseShort(((StringData) response.getValue()).getData());
            } catch (NumberFormatException | ClassCastException e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean exists(String p_name) {
        RequestResponse response = sendRequest(m_requestFactory.getReadRequest(p_name));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean write(String p_name, RaftData p_value, boolean p_overwrite) {
        RequestResponse response = sendRequest(m_requestFactory.getWriteRequest(p_name, p_value, p_overwrite));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean write(String p_name, short p_value, boolean p_overwrite) {
        RequestResponse response = sendRequest(
            m_requestFactory.getWriteRequest(p_name, new StringData(String.valueOf(p_value)), p_overwrite));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean compareAndSet(String p_name, RaftData p_value, RaftData p_compareValue) {
        RequestResponse response = sendRequest(m_requestFactory.getCompareAndSetRequest(p_name, p_value, p_compareValue));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public RaftData delete(String p_name) {
        RequestResponse response = sendRequest(m_requestFactory.getDeleteRequest(p_name));
        if (response != null && response.isSuccess()) {
            return response.getValue();
        }

        return null;
    }

    public List<RaftData> readList(String p_name) {
        RequestResponse response = sendRequest(m_requestFactory.getReadListRequest(p_name));
        if (response != null && response.isSuccess()) {
            return ((ListData) response.getValue()).getData();
        }

        return null;
    }

    public boolean listExists(String p_name) {
        RequestResponse response = sendRequest(m_requestFactory.getReadListRequest(p_name));
        if (response != null && response.isSuccess()) {
            return !((ListData) response.getValue()).getData().isEmpty();
        }

        return false;
    }

    public boolean writeList(String p_name, List<RaftData> p_value, boolean p_overwrite) {
        RequestResponse response = sendRequest(m_requestFactory.getWriteListRequest(p_name, p_value, p_overwrite));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean addToList(String p_name, RaftData p_value) {
        RequestResponse response = sendRequest(m_requestFactory.getAppendToListRequest(p_name, p_value));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public boolean removeFromList(String p_name, RaftData p_value) {
        RequestResponse response = sendRequest(m_requestFactory.getDeleteFromListRequest(p_name, p_value));
        if (response != null) {
            return response.isSuccess();
        }

        return false;
    }

    public List<RaftData> deleteList(String p_name) {
        RequestResponse response = sendRequest(m_requestFactory.getDeleteListRequest(p_name));
        if (response != null && response.isSuccess()) {
            return ((ListData) response.getValue()).getData();
        }

        return null;
    }

    public boolean addServer(RaftAddress p_server) {
        RequestResponse response = sendRequest(m_requestFactory.getAddServerRequest(p_server));
        if (response != null) {
            if (response.isSuccess()) {
                m_context.getRaftServers().add(p_server);
            }
            return response.isSuccess();
        }

        return false;
    }

    public boolean removeServer(RaftAddress p_server) {
        RequestResponse response = sendRequest(m_requestFactory.getRemoveServerRequest(p_server));
        if (response != null) {
            if (response.isSuccess()) {
                m_context.getRaftServers().remove(p_server);
            }
            return response.isSuccess();
        }

        return false;
    }

    public RaftAddress getCurrentLeader() {
        RequestResponse response = sendRequest(m_requestFactory.getReadRequest(SpecialPaths.LEADER_PATH));
        if (response != null && response.isSuccess()) {
            return (RaftAddress) response.getValue();
        }

        return null;
    }

    public List<RaftAddress> getCurrentConfig() {
        RequestResponse response = sendRequest(m_requestFactory.getReadListRequest(SpecialPaths.CLUSTER_CONFIG_PATH));
        if (response != null && response.isSuccess()) {
            return ((ListData) response.getValue()).getData().stream().map(data -> (RaftAddress) data).collect(Collectors.toList());
        }

        return null;
    }

    public void shutdown() {
        m_networkService.close();
    }

    private RequestResponse sendRequest(Request p_request) {
        RaftAddress serverAddress = getRandomServer();
        long startTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        while (startTime - currentTime + m_context.getOverallTryDuration() > 0) {

            LOGGER.debug("Sending request to server {}", serverAddress);

            p_request.setReceiverAddress(serverAddress);
            RequestResponse response = m_networkService.sendRequest(p_request);

            currentTime = System.currentTimeMillis();

            if (response == null) {
                LOGGER.debug("No response or other communication problem with server {}", serverAddress);
                serverAddress = getRandomServer();
                continue;
            }

            if (response.isRedirection()) {
                LOGGER.debug("Received redirection to server {}", response.getLeaderAddress());

                if (response.getLeaderAddress() != null) {
                    serverAddress = response.getLeaderAddress();
                } else {
                    // cluster might be trying to elect leader
                    // wait a short time
                    try {
                        Thread.sleep(m_context.getRetryTimeout());
                    } catch (InterruptedException e) {
                        LOGGER.error(e);
                    }
                    serverAddress = getRandomServer();
                }
            } else {
                LOGGER.debug("Received response");
                return response;
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


        String configPath = System.getProperty("dxraft.config");
        if (configPath == null) {
            configPath = CLIENT_CONFIG_PATH;
        }

        ClientConfig config = ConfigUtils.getClientConfig(configPath);

        if (config == null) {
            return;
        }

        RaftClient client = new RaftClient(config);

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
                    short id = Short.parseShort(strings[1]);
                    String ip = strings[2];
                    int port = Integer.parseInt(strings[3]);
                    System.out.println("Adding server with id " + id + " running at " + ip + ':' + port + "...");
                    RaftAddress address = new RaftAddress(id, ip, -1, port);
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
                    short id = Short.parseShort(strings[1]);
                    String ip = strings[2];
                    int port = Integer.parseInt(strings[3]);
                    System.out.println("Removing server with id " + id + " running at " + ip + ':' + port + "...");
                    RaftAddress address = new RaftAddress(id, ip, -1, port);
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
