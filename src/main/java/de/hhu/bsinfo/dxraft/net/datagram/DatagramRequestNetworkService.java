package de.hhu.bsinfo.dxraft.net.datagram;

import de.hhu.bsinfo.dxraft.client.message.Request;
import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.server.ServerConfig;
import de.hhu.bsinfo.dxraft.server.message.RequestResponse;
import de.hhu.bsinfo.dxraft.server.net.AbstractRequestNetworkService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class DatagramRequestNetworkService extends AbstractRequestNetworkService {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int MAX_MESSAGE_SIZE = 65535;
    private ServerConfig m_context;
    private DatagramSocket m_socket;
    private Thread m_receiverThread;

    public DatagramRequestNetworkService(ServerConfig p_context) {
        m_context = p_context;

        try  {
            m_socket = new DatagramSocket(
                new InetSocketAddress(m_context.getIp(), m_context.getRequestPort()));
        } catch (IOException e) {
            LOGGER.error("Exception creating sockets", e);
        }    }

    @Override
    public void sendResponse(RequestResponse p_message) {
        DatagramPacket packet = preparePacket(p_message);
        if (packet != null) {
            try {
                m_socket.send(packet);
            } catch (IOException e) {
                LOGGER.error("Exception sending message", e);
            }
        }
    }

    @Override
    public void startReceiving() {
        m_receiverThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                Request request = receiveMessage();
                if (request != null) {
                    getRequestReceiver().processClientRequest(request);
                }

            }
        });

        m_receiverThread.start();
    }

    @Override
    public void stopReceiving() {
        m_receiverThread.interrupt();
    }

    @Override
    public void close() {
        if (m_socket != null) {
            m_socket.close();
        }
    }

    private DatagramPacket preparePacket(RequestResponse p_message) {
        RaftAddress address = m_context.getRaftAddress();
        p_message.setSenderAddress(address);

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

    private Request receiveMessage() {
        try {
            byte[] buf = new byte[MAX_MESSAGE_SIZE];
            DatagramPacket msg = new DatagramPacket(buf, buf.length);
            m_socket.setSoTimeout(0);
            m_socket.receive(msg);
            ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(msg.getData()));
            Request message = (Request) objIn.readObject();

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
