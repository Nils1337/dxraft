package de.hhu.bsinfo.dxraft.test;

import de.hhu.bsinfo.dxraft.client.ClientNetworkService;
import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.message.*;
import de.hhu.bsinfo.dxraft.server.ServerNetworkService;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class DatagramNetworkService extends ServerNetworkService implements ClientNetworkService {

    private static final int MAX_MESSAGE_SIZE = 65535;
    private RaftContext context;
    private DatagramSocket socket;
    private Thread receiverThread;
    private Queue<RaftMessage> clientResponses = new LinkedBlockingQueue<>();

    public DatagramNetworkService(RaftContext context) {
        this.context = context;

        try  {
            socket = new DatagramSocket(new InetSocketAddress(context.getLocalAddress().getIp(), context.getLocalAddress().getPort()));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public RaftClientMessage sendRequest(ClientRequest request) {
        sendMessage(request);
        return (RaftClientMessage) receiveMessage();
    }

    @Override
    public void sendMessage(RaftMessage message) {
        try (
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(out)
        )
        {
            objOut.writeObject(message);
            byte[] msg = out.toByteArray();

            RaftAddress receiverAddress = context.getAddressById(message.getReceiverId());
            SocketAddress socketAddress = new InetSocketAddress(receiverAddress.getIp(), receiverAddress.getPort());

            socket.send(new DatagramPacket(msg, msg.length, socketAddress));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendMessageToAllServers(RaftMessage message) {
        for (RaftID id : context.getRaftServers()) {
            message.setReceiverId(id);
            sendMessage(message);
        }
    }

    @Override
    public void start() {

        receiverThread = new Thread(() -> {
            while (true) {
                RaftMessage message = receiveMessage();
                if (message instanceof MessageDeliverer) {
                    ((MessageDeliverer) message).deliverMessage(messageReceiver);
                }
            }
        });

        receiverThread.start();

    }

    @Override
    public void stop() {
        receiverThread.interrupt();
    }

    private RaftMessage receiveMessage() {
        try {
            byte[] buf = new byte[MAX_MESSAGE_SIZE];
            DatagramPacket msg = new DatagramPacket(buf, buf.length);
            socket.receive(msg);
            ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(msg.getData()));
            return (RaftMessage) objIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
