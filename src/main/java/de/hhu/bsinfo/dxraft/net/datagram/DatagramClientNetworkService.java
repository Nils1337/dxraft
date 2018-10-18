package de.hhu.bsinfo.dxraft.net.datagram;

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

import de.hhu.bsinfo.dxraft.client.message.Request;
import de.hhu.bsinfo.dxraft.client.net.ClientNetworkService;
import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.server.message.RequestResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatagramClientNetworkService implements ClientNetworkService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_MESSAGE_SIZE = 65535;

    private DatagramSocket m_clientSocket;

    public DatagramClientNetworkService() {
        try {
            m_clientSocket = new DatagramSocket();
        } catch (SocketException e) {
            LOGGER.error("Error opening socket", e);
        }
    }

    @Override
    public RequestResponse sendRequest(Request p_request) {
        DatagramPacket packet = preparePacket(p_request);
        if (packet != null) {
            try {
                m_clientSocket.send(packet);

                boolean received = false;
                RequestResponse msg = null;
                while (!received) {
                    msg = receive();
                    received = true;

                    // response could be for another (old) request issued by this client
                    // e.g. a request was committed on leader but crashed immediately after,
                    // so next leader does not know this entry is committed
                    // ignore other responses for now
                    if (msg != null && !msg.isRedirection()) {
                        if (!msg.getRequestId().equals(p_request.getId())) {
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

    @Override
    public void close() {
        if (m_clientSocket != null) {
            m_clientSocket.close();
        }
    }

    private RequestResponse receive() {
        try {
            byte[] buf = new byte[MAX_MESSAGE_SIZE];
            DatagramPacket msg = new DatagramPacket(buf, buf.length);
            m_clientSocket.setSoTimeout(1000);
            m_clientSocket.receive(msg);
            ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(msg.getData()));
            return (RequestResponse) objIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.trace("Error receiving message");
        }
        return null;
    }

    private DatagramPacket preparePacket(Request p_message) {
        try (
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(out)
        )
        {
            objOut.writeObject(p_message);
            byte[] msg = out.toByteArray();
            RaftAddress receiverAddress = p_message.getReceiverAddress();

            if (receiverAddress == null) {
                LOGGER.error("Receiver of message {} could not be determined", p_message);
                return null;
            }

            SocketAddress socketAddress = new InetSocketAddress(receiverAddress.getIp(), receiverAddress.getRequestPort());
            return new DatagramPacket(msg, msg.length, socketAddress);
        } catch (IOException e) {
            LOGGER.error("Exception preparing message", e);
        }

        return null;
    }
}
