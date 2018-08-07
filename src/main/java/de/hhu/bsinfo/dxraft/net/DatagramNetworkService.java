package de.hhu.bsinfo.dxraft.net;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.message.*;
import de.hhu.bsinfo.dxraft.message.client.ClientRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;

public class DatagramNetworkService extends AbstractNetworkService {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int MAX_MESSAGE_SIZE = 65535;
    private RaftContext context;
    private DatagramSocket serverSocket;
    private DatagramSocket clientSocket;
    private Thread receiverThread;

    public DatagramNetworkService(RaftContext context, boolean clientOnly) {
        this.context = context;

        try  {
            if (!clientOnly) {
                serverSocket = new DatagramSocket(
                    new InetSocketAddress(context.getLocalAddress().getIp(), context.getLocalAddress().getPort()));
            }
            clientSocket = new DatagramSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public RaftMessage sendRequest(ClientRequest request) {
        DatagramPacket packet = preparePacket(request);
        if (packet != null) {
            try {
                clientSocket.send(packet);
                return receiveMessageClient();
            } catch (IOException e) {
                LOGGER.error("Error sending message", e);
            }
        }
        return null;
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
                RaftMessage message = receiveMessageServer();
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

    private DatagramPacket preparePacket(RaftMessage message) {

        RaftAddress address = context.getLocalAddress();

        // server messages should have the port set, client messages not
        // set the port to the randomly chosen one of the client socket
        if (address.getPort() == -1) {
            address.setPort(clientSocket.getLocalPort());
        }

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
    private RaftMessage receiveMessageClient() {
        try {
            byte[] buf = new byte[MAX_MESSAGE_SIZE];
            DatagramPacket msg = new DatagramPacket(buf, buf.length);
            clientSocket.setSoTimeout(1000);
            clientSocket.receive(msg);
            ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(msg.getData()));
            return (RaftMessage) objIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.trace("Error receiving message");
        }
        return null;
    }

    private RaftMessage receiveMessageServer() {
        try {
            byte[] buf = new byte[MAX_MESSAGE_SIZE];
            DatagramPacket msg = new DatagramPacket(buf, buf.length);
            serverSocket.setSoTimeout(0);
            serverSocket.receive(msg);
            ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(msg.getData()));
            return (RaftMessage) objIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.trace("Error receiving message");
        }
        return null;
    }
}
