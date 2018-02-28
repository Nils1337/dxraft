package de.hhu.bsinfo.dxraft.net;

import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.message.RaftMessage;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class LocalTestNetworkService extends RaftNetworkService {

    private Map<Short, LinkedBlockingQueue<RaftMessage>> messageQueues;
    private RaftContext context;
    private int networkRandomization;

    private Thread messagePollingThread = new Thread(() -> {
        try {
            while(true) {
                RaftMessage message = messageQueues.get(context.getLocalId()).poll(10, TimeUnit.SECONDS);
                if (message != null) {
                    // emulate network delay

                    int networkDelay = ThreadLocalRandom.current().nextInt(0, networkRandomization);
                    try {
                        Thread.sleep(networkDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    System.out.println("Delivering " + message.getClass() + " from " + message.getSenderId() + " to " + message.getReceiverId() + " with term " + message.getTerm());
                    message.processMessage(messageReceiver);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    });

    public LocalTestNetworkService(RaftContext context, Map<Short, LinkedBlockingQueue<RaftMessage>> messageQueues, int networkRandomization) {
        this.context = context;
        this.messageQueues = messageQueues;
        this.networkRandomization = networkRandomization;
    }

    @Override
    public void sendMessage(RaftMessage message) {
        messageQueues.get(message.getReceiverId()).add(message);
    }

    @Override
    public void start() {
        messagePollingThread.start();
    }

    @Override
    public void stop() {
        messagePollingThread.interrupt();
    }
}
