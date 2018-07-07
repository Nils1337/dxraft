package de.hhu.bsinfo.dxraft.net;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.message.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;

public class DatagramNetworkService extends AbstractNetworkService {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int MAX_MESSAGE_SIZE = 65535;
    private RaftContext context;
    private DatagramSocket socket;
    private Thread receiverThread;

    public DatagramNetworkService(RaftContext context) {
        this.context = context;

        try  {
            socket = new DatagramSocket(new InetSocketAddress(context.getLocalAddress().getIp(), context.getLocalAddress().getPort()));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public RaftMessage sendRequest(ClientRequest request) {
        sendMessage(request);
        return receiveMessageWithTimeout();
    }

    @Override
    public void sendMessage(RaftMessage message) {
        message.setSenderAddress(context.getLocalAddress());

        try (
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(out)
        )
        {
            objOut.writeObject(message);
            byte[] msg = out.toByteArray();

            RaftAddress receiverAddress = message.getReceiverAddress();

            // if address is not set, try to get it from the context by id
            if (receiverAddress == null) {
                receiverAddress = context.getAddressById(message.getReceiverId());
            }

            if (receiverAddress == null) {
                LOGGER.error("Receiver of message " + message + " could not be determined");
                return;
            }

            SocketAddress socketAddress = new InetSocketAddress(receiverAddress.getIp(), receiverAddress.getPort());

            socket.send(new DatagramPacket(msg, msg.length, socketAddress));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendMessageToAllServers(RaftMessage message) {
        for (RaftID id : context.getOtherServerIds()) {
            message.setReceiverId(id);
            sendMessage(message);
        }
    }

    @Override
    public void startReceiving() {

        receiverThread = new Thread(() -> {
            while (true) {
                RaftMessage message = receiveMessage();
                if (message instanceof MessageDeliverer) {
                    ((MessageDeliverer) message).deliverMessage(getMessageReceiver());
                }
            }
        });

        receiverThread.start();

    }

    @Override
    public void stopReceiving() {
        receiverThread.interrupt();
    }

    private RaftMessage receiveMessageWithTimeout() {
        try {
            byte[] buf = new byte[MAX_MESSAGE_SIZE];
            DatagramPacket msg = new DatagramPacket(buf, buf.length);
            socket.setSoTimeout(1000);
            socket.receive(msg);
            ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(msg.getData()));
            return (RaftMessage) objIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.trace("Error receiving message");
        }
        return null;
    }

    private RaftMessage receiveMessage() {
        try {
            byte[] buf = new byte[MAX_MESSAGE_SIZE];
            DatagramPacket msg = new DatagramPacket(buf, buf.length);
            socket.setSoTimeout(0);
            socket.receive(msg);
            ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(msg.getData()));
            return (RaftMessage) objIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.trace("Error receiving message");
        }
        return null;
    }
}
