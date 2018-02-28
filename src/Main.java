import de.hhu.bsinfo.dxraft.RaftServer;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.message.RaftMessage;
import de.hhu.bsinfo.dxraft.net.LocalTestNetworkService;
import de.hhu.bsinfo.dxraft.net.RaftNetworkService;

import java.util.*;
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

        List<Short> ids = new ArrayList<>();
        Map<Short, LinkedBlockingQueue<RaftMessage>> messageQueues = new HashMap<>(SERVER_COUNT);
        RaftServer[] servers = new RaftServer[SERVER_COUNT];

        for (short i = 1; i < SERVER_COUNT+1; i++) {
            ids.add(i);
        }

        for (short i = 1; i < SERVER_COUNT+1; i++) {
            messageQueues.put(i, new LinkedBlockingQueue<>());
            servers[i-1] = createTestServer(i, ids, messageQueues);
        }

        for (short i = 1; i < SERVER_COUNT+1; i++) {
            servers[i-1].start();
        }

    }

    private static RaftServer createTestServer(short id, List<Short> ids, Map<Short, LinkedBlockingQueue<RaftMessage>> messageQueues) {
        List<Short> localIds = new ArrayList<>(ids);
        RaftContext context = new RaftContext(localIds, FOLLOWER_TIMEOUT_DURATION, FOLLOWER_RANDOMIZATION_AMOUNT, ELECTION_TIMEOUT_DURATION, ELECTION_RANDOMIZATION_AMOUNT, HEARTBEAT_TIMEOUT_DURATION, HEARTBEAT_RANDOMIZATION_AMOUNT, id);
        RaftNetworkService networkService = new LocalTestNetworkService(context, messageQueues, NETWORK_DELAY_RANDOMIZATION);
        return new RaftServer(context, networkService);
    }
}
