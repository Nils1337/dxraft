import de.hhu.bsinfo.dxraft.client.RaftClient;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.data.StringData;
import de.hhu.bsinfo.dxraft.message.*;
import de.hhu.bsinfo.dxraft.server.RaftServer;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.test.LocalTestNetworkService;
import de.hhu.bsinfo.dxraft.server.ServerNetworkService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {

    private static final int SERVER_COUNT = 9;
    private static final int NETWORK_DELAY_RANDOMIZATION = 5;

    // timeout duration and randomization amount when following leader
    private static final int FOLLOWER_TIMEOUT_DURATION = 500;
    private static final int FOLLOWER_RANDOMIZATION_AMOUNT = 50;

    // timeout duration and randomization amount when electing
    private static final int ELECTION_TIMEOUT_DURATION = 500;
    private static final int ELECTION_RANDOMIZATION_AMOUNT = 50;

    // timeout duration and randomization amount of leader
    private static final int HEARTBEAT_TIMEOUT_DURATION = 100;
    private static final int HEARTBEAT_RANDOMIZATION_AMOUNT = 0;

    public static void main(String[] args) {

        List<RaftID> serverIds = new ArrayList<>();
        for (short i = 1; i < SERVER_COUNT+1; i++) {
            serverIds.add(new RaftID(i));
        }

        List<RaftID> clientIds = new ArrayList<>();
        clientIds.add(new RaftID((short) (SERVER_COUNT + 1)));

        Map<RaftID, LinkedBlockingQueue<MessageDeliverer>> messageQueues = new HashMap<>(SERVER_COUNT);

        for (short i = 1; i < SERVER_COUNT+1; i++) {
            messageQueues.put(new RaftID(i), new LinkedBlockingQueue<>());
        }

        Map<RaftID, RaftClientMessage> responseMap = new ConcurrentHashMap<>(1);

        RaftServer[] servers = new RaftServer[SERVER_COUNT];
        for (short i = 1; i < SERVER_COUNT+1; i++) {
            servers[i-1] = createTestServer(new RaftID(i), serverIds, clientIds, messageQueues, responseMap);
        }

        for (short i = 1; i < SERVER_COUNT+1; i++) {
            servers[i-1].start();
        }

        RaftContext context = new RaftContext(serverIds, clientIds, new RaftID((short) (SERVER_COUNT + 1)));
        RaftClient client = new RaftClient(context, new LocalTestNetworkService(context, messageQueues, responseMap, NETWORK_DELAY_RANDOMIZATION));

        while (true) {
            System.out.print(">> ");
            Scanner scanner = new Scanner(System.in);
            String in = scanner.nextLine();

            if (in.startsWith("read")) {
                String[] strings = in.split(" ");
                if (strings.length > 1) {
                    Object result = client.read(strings[1]);
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
                    Object result = client.delete(strings[1]);
                    if (result != null) {
                        System.out.println("Deletion of " + result + " under \"" + strings[1] + "\" was successful!");
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

    private static RaftServer createTestServer(RaftID id, List<RaftID> serverIds, List<RaftID> clientIds, Map<RaftID, LinkedBlockingQueue<MessageDeliverer>> messageQueues, Map<RaftID, RaftClientMessage> responseMap) {
        List<RaftID> localIds = new ArrayList<>(serverIds);
        RaftServerContext context = new RaftServerContext(localIds, clientIds, id, FOLLOWER_TIMEOUT_DURATION, FOLLOWER_RANDOMIZATION_AMOUNT, ELECTION_TIMEOUT_DURATION, ELECTION_RANDOMIZATION_AMOUNT, HEARTBEAT_TIMEOUT_DURATION, HEARTBEAT_RANDOMIZATION_AMOUNT);
        ServerNetworkService networkService = new LocalTestNetworkService(context, messageQueues, responseMap, NETWORK_DELAY_RANDOMIZATION);
        return new RaftServer(context, networkService);
    }
}
