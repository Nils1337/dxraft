package de.hhu.bsinfo.dxraft.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.message.RaftMessage;
import de.hhu.bsinfo.dxraft.message.client.ClientRequest;
import de.hhu.bsinfo.dxraft.message.server.ClientRedirection;
import de.hhu.bsinfo.dxraft.message.server.ClientResponse;

public class ClientDatagramNetworkService implements ClientNetworkService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_MESSAGE_SIZE = 65535;

    private DatagramSocket clientSocket;

    public ClientDatagramNetworkService() {
        try {
            clientSocket = new DatagramSocket();
        } catch (SocketException e) {
            LOGGER.error("Error opening socket", e);
        }
    }

    @Override
    public RaftMessage sendRequest(ClientRequest request) {
        DatagramPacket packet = preparePacket(request);
        if (packet != null) {
            try {
                clientSocket.send(packet);

                boolean received = false;
                RaftMessage msg = null;
                while (!received) {
                    msg = receive();
                    received = true;

                    // response could be for another (old) request issued by this client
                    // e.g. a request was committed on leader but crashed immediately after,
                    // so next leader does not know this entry is committed
                    // ignore other responses for now
                    if (msg instanceof ClientResponse) {
                        ClientResponse response = (ClientResponse) msg;
                        if (!response.getRequestId().equals(request.getId())) {
                            LOGGER.debug("received response for old request, waiting for response for current request");
                            received = false;
                        }
                    }
                }

                return msg;

            } catch (IOException e) {
                LOGGER.error("Error sending message", e);
            }
        }
        return null;
    }

    public void close() {
        if (clientSocket != null) {
            clientSocket.close();
        }
    }

    private RaftMessage receive() {
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

    private DatagramPacket preparePacket(RaftMessage message) {
        try (
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(out)
        )
        {
            objOut.writeObject(message);
            byte[] msg = out.toByteArray();
            RaftAddress receiverAddress = message.getReceiverAddress();

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
}