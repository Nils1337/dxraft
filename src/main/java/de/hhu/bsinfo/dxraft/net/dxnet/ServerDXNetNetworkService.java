package de.hhu.bsinfo.dxraft.net.dxnet;

import de.hhu.bsinfo.dxnet.DXNet;
import de.hhu.bsinfo.dxnet.MessageReceiver;
import de.hhu.bsinfo.dxnet.core.CoreConfig;
import de.hhu.bsinfo.dxnet.core.NetworkException;
import de.hhu.bsinfo.dxnet.core.messages.Messages;
import de.hhu.bsinfo.dxnet.ib.IBConfig;
import de.hhu.bsinfo.dxnet.loopback.LoopbackConfig;
import de.hhu.bsinfo.dxnet.nio.NIOConfig;
import de.hhu.bsinfo.dxraft.server.ServerConfig;
import de.hhu.bsinfo.dxraft.server.message.ServerMessage;
import de.hhu.bsinfo.dxraft.server.net.AbstractServerNetworkService;

public class ServerDXNetNetworkService extends AbstractServerNetworkService {
    @Override
    public void sendMessage(ServerMessage p_message) {

    }

    @Override
    public void startReceiving() {

    }

    @Override
    public void stopReceiving() {

    }

    @Override
    public void close() {

    }

//    private DXNet m_dxnet;
//    private MessageReceiver m_msgReceiver = p_message ->
//        ((AbstractServerMessage) p_message).deliverMessage(getMessageReceiver());
//
//    public ServerDXNetNetworkService(ServerConfig p_context) {
//        m_dxnet = new DXNet(new CoreConfig(), new NIOConfig(), new IBConfig(), new LoopbackConfig(), new NodeMappings(p_context));
//    }
//
//    @Override
//    public void sendMessage(AbstractServerMessage p_message) {
//        try {
//            m_dxnet.sendMessage(p_message);
//        } catch (NetworkException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void startReceiving() {
//        m_dxnet.register(Messages.DEFAULT_MESSAGES_TYPE, RaftMessages.SUBTYPE_SERVER_MESSAGE_WRAPPER,
//            p_message -> ((AbstractServerMessage) p_message).deliverMessage(getMessageReceiver()));
//        m_dxnet.registerSpecialReceiveMessageType();
//    }
//
//    @Override
//    public void stopReceiving() {
//        m_dxnet.unregister(Messages.DEFAULT_MESSAGES_TYPE, RaftMessages.SUBTYPE_SERVER_MESSAGE_WRAPPER,
//            p_message -> ((AbstractServerMessage) p_message).deliverMessage(getMessageReceiver()));
//    }
//
//    @Override
//    public void close() {
//        m_dxnet.close();
//    }
}
