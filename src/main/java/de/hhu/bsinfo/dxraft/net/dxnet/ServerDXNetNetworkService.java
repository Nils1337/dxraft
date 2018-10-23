package de.hhu.bsinfo.dxraft.net.dxnet;

import de.hhu.bsinfo.dxnet.DXNet;
import de.hhu.bsinfo.dxnet.MessageReceiver;
import de.hhu.bsinfo.dxnet.core.NetworkException;
import de.hhu.bsinfo.dxnet.loopback.LoopbackConfig;
import de.hhu.bsinfo.dxraft.net.dxnet.message.*;
import de.hhu.bsinfo.dxraft.server.ServerConfig;
import de.hhu.bsinfo.dxraft.server.message.ServerMessage;
import de.hhu.bsinfo.dxraft.server.net.AbstractServerNetworkService;

public class ServerDXNetNetworkService extends AbstractServerNetworkService {

    private DXNet m_dxnet;
    private ServerConfig m_context;
    private MessageReceiver m_msgReceiver = p_message ->
        ((AbstractDXNetServerMessage) p_message).deliverMessage(getMessageReceiver());

    public ServerDXNetNetworkService(ServerConfig p_context) {
        p_context.getDxnetCoreConfig().setOwnNodeId(p_context.getLocalId());
        m_dxnet = new DXNet(p_context.getDxnetCoreConfig(), p_context.getDxnetNioConfig(), p_context.getDxnetIbConfig(),
            new LoopbackConfig(), new NodeMappings(p_context));
        m_context = p_context;

        m_dxnet.registerMessageType(RaftMessages.DXRAFT_MESSAGE, RaftMessages.APPEND_ENTRIES_REQUEST,
            DXNetAppendEntriesRequest.class);
        m_dxnet.registerMessageType(RaftMessages.DXRAFT_MESSAGE, RaftMessages.APPEND_ENTRIES_RESPONSE,
            DXNetAppendEntriesResponse.class);
        m_dxnet.registerMessageType(RaftMessages.DXRAFT_MESSAGE, RaftMessages.VOTE_REQUEST,
            DXNetVoteRequest.class);
        m_dxnet.registerMessageType(RaftMessages.DXRAFT_MESSAGE, RaftMessages.VOTE_RESPONSE,
            DXNetVoteResponse.class);
    }

    @Override
    public void sendMessage(ServerMessage p_message) {
        try {
            p_message.setSenderId(m_context.getLocalId());
            m_dxnet.sendMessage((AbstractDXNetServerMessage) p_message);
        } catch (NetworkException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startReceiving() {
        m_dxnet.register(RaftMessages.DXRAFT_MESSAGE, RaftMessages.APPEND_ENTRIES_REQUEST,
            m_msgReceiver);
        m_dxnet.register(RaftMessages.DXRAFT_MESSAGE, RaftMessages.APPEND_ENTRIES_RESPONSE,
            m_msgReceiver);
        m_dxnet.register(RaftMessages.DXRAFT_MESSAGE, RaftMessages.VOTE_REQUEST,
            m_msgReceiver);
        m_dxnet.register(RaftMessages.DXRAFT_MESSAGE, RaftMessages.VOTE_RESPONSE,
            m_msgReceiver);
    }

    @Override
    public void stopReceiving() {
        m_dxnet.unregister(RaftMessages.DXRAFT_MESSAGE, RaftMessages.APPEND_ENTRIES_REQUEST,
            m_msgReceiver);
        m_dxnet.unregister(RaftMessages.DXRAFT_MESSAGE, RaftMessages.APPEND_ENTRIES_RESPONSE,
            m_msgReceiver);
        m_dxnet.unregister(RaftMessages.DXRAFT_MESSAGE, RaftMessages.VOTE_REQUEST,
            m_msgReceiver);
        m_dxnet.unregister(RaftMessages.DXRAFT_MESSAGE, RaftMessages.VOTE_RESPONSE,
            m_msgReceiver);
    }

    @Override
    public void close() {
        m_dxnet.close();
    }
}
