package de.hhu.bsinfo.dxraft.test;

import de.hhu.bsinfo.dxraft.client.ClientNetworkService;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.message.*;
import de.hhu.bsinfo.dxraft.server.ServerNetworkService;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class LocalTestNetworkService extends ServerNetworkService implements ClientNetworkService {

    private Map<RaftID, LinkedBlockingQueue<MessageDeliverer>> messageQueues;
    private Map<RaftID, RaftClientMessage> responseMap;
    private RaftContext context;
    private int networkRandomization;

    private Thread messagePollingThread = new Thread(() -> {
        try {
            while(true) {
                MessageDeliverer message = messageQueues.get(context.getLocalId()).poll(10, TimeUnit.SECONDS);
                if (message != null) {
                    // emulate network delay

                    int networkDelay = ThreadLocalRandom.current().nextInt(0, networkRandomization);
                    try {
                        Thread.sleep(networkDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                    //System.out.println("Delivering " + message.getClass() + " from " + message.getSenderId() + " to " + message.getReceiverId() + " with term " + message.getTerm());

                    message.deliverMessage(messageReceiver);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    });

    public LocalTestNetworkService(RaftContext context, Map<RaftID, LinkedBlockingQueue<MessageDeliverer>> messageQueues, Map<RaftID, RaftClientMessage> responseMap, int networkRandomization) {
        this.context = context;
        this.messageQueues = messageQueues;
        this.responseMap = responseMap;
        this.networkRandomization = networkRandomization;
    }

    @Override
    public void sendMessage(RaftMessage message) {
        if (message instanceof RaftClientMessage) {
            responseMap.put(message.getReceiverId(), (RaftClientMessage) message);
        } else if (message instanceof RaftServerMessage){
            RaftServerMessage serverMessage = (RaftServerMessage) message;
            Queue<MessageDeliverer> receiverQueue = messageQueues.get(message.getReceiverId());
            if (receiverQueue == null) {
                throw new IllegalArgumentException("Message receiver is not a raft server!");
            }
            receiverQueue.add(serverMessage);
        }
    }

    @Override
    public void sendMessageToAllServers(RaftMessage message) {

    }

    @Override
    public void start() {
        messagePollingThread.start();
    }

    @Override
    public void stop() {
        messagePollingThread.interrupt();
    }

    @Override
    public RaftClientMessage sendRequest(ClientRequest request) {
        messageQueues.get(request.getReceiverId()).add(request);
        RaftClientMessage response;
        while ((response = responseMap.remove(request.getSenderId())) == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return response;
    }
}
