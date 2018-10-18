package de.hhu.bsinfo.dxraft.net.datagram;

import de.hhu.bsinfo.dxraft.data.RaftAddress;

import de.hhu.bsinfo.dxraft.server.ServerConfig;
import de.hhu.bsinfo.dxraft.server.message.ServerMessage;
import de.hhu.bsinfo.dxraft.server.message.ServerMessageDeliverer;
import de.hhu.bsinfo.dxraft.server.net.AbstractServerNetworkService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;

public class DatagramServerNetworkService extends AbstractServerNetworkService {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int MAX_MESSAGE_SIZE = 65535;
    private ServerConfig context;
    private DatagramSocket serverSocket;
    private Thread receiverThread;

    public DatagramServerNetworkService(ServerConfig context) {
        this.context = context;

        try  {
            serverSocket = new DatagramSocket(
                new InetSocketAddress(context.getIp(), context.getRaftPort()));
        } catch (IOException e) {
            LOGGER.error("Exception creating sockets", e);
        }
    }


    @Override
    public void sendMessage(ServerMessage p_message) {
        DatagramPacket packet = preparePacket(p_message);
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
                ServerMessage message = receiveMessage();
                if (message != null) {
                    ((ServerMessageDeliverer) message).deliverMessage(getMessageReceiver());
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

    private DatagramPacket preparePacket(ServerMessage message) {
        RaftAddress address = context.getRaftAddress();
        message.setSenderAddress(address);
        message.setSenderId(context.getLocalId());

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

            SocketAddress socketAddress = new InetSocketAddress(receiverAddress.getIp(), receiverAddress.getInternalPort());
            return new DatagramPacket(msg, msg.length, socketAddress);

        } catch (IOException e) {
            LOGGER.error("Exception preparing message", e);
        }

        return null;
    }

    private ServerMessage receiveMessage() {
        try {
            byte[] buf = new byte[MAX_MESSAGE_SIZE];
            DatagramPacket msg = new DatagramPacket(buf, buf.length);
            serverSocket.receive(msg);
            ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(msg.getData()));
            ServerMessage message = (ServerMessage) objIn.readObject();

            // set sender if not already set on sender side
            if (message.getSenderAddress() == null) {
                RaftAddress address = new RaftAddress(RaftAddress.INVALID_ID, msg.getAddress().getHostAddress(), msg.getPort(), -1);
                message.setSenderAddress(address);
            }

            return message;
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.trace("Error receiving message");
        }
        return null;
    }
}
