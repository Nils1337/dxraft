package de.hhu.bsinfo.dxraft.net;

import de.hhu.bsinfo.dxraft.message.*;
import de.hhu.bsinfo.dxraft.server.ServerContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;

public class ServerDatagramNetworkService extends AbstractServerNetworkService {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int MAX_MESSAGE_SIZE = 65535;
    private ServerContext context;
    private DatagramSocket serverSocket;
    private Thread receiverThread;

    public ServerDatagramNetworkService(ServerContext context) {
        this.context = context;

        try  {
            serverSocket = new DatagramSocket(
                new InetSocketAddress(context.getLocalAddress().getIp(), context.getLocalAddress().getPort()));
        } catch (IOException e) {
            LOGGER.error("Exception creating sockets", e);
        }
    }


    @Override
    public void sendMessage(RaftMessage message) {
        DatagramPacket packet = preparePacket(message);
        if (packet != null) {
            try {
                serverSocket.send(packet);
            } catch (IOException e) {
                LOGGER.error("Exception sending message", e);
            }
        }
    }

    @Override
    public void startReceiving() {

        receiverThread = new Thread(() -> {
            while (!Thread.interrupted()) {
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

    @Override
    public void close() {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    private DatagramPacket preparePacket(RaftMessage message) {
        RaftAddress address = context.getLocalAddress();
        message.setSenderAddress(address);

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
                return null;
            }

            SocketAddress socketAddress = new InetSocketAddress(receiverAddress.getIp(), receiverAddress.getPort());
            return new DatagramPacket(msg, msg.length, socketAddress);

        } catch (IOException e) {
            LOGGER.error("Exception preparing message", e);
        }

        return null;
    }

    private RaftMessage receiveMessage() {
        try {
            byte[] buf = new byte[MAX_MESSAGE_SIZE];
            DatagramPacket msg = new DatagramPacket(buf, buf.length);
            serverSocket.setSoTimeout(0);
            serverSocket.receive(msg);
            ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(msg.getData()));
            RaftMessage message = (RaftMessage) objIn.readObject();

            // set sender if not already set on sender side
            if (message.getSenderAddress() == null) {
                message.setSenderAddress(new RaftAddress(msg.getAddress().getHostAddress(), msg.getPort()));
            }

            return message;
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.trace("Error receiving message");
        }
        return null;
    }
}
