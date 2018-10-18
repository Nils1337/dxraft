package de.hhu.bsinfo.dxraft.net.dxnet;

import de.hhu.bsinfo.dxnet.NodeMap;
import de.hhu.bsinfo.dxraft.server.ServerConfig;

import java.net.InetSocketAddress;
import java.util.List;

public class NodeMappings implements NodeMap {

    private ServerConfig m_context;

    public NodeMappings(ServerConfig p_context) {
        m_context = p_context;
    }

    @Override
    public short getOwnNodeID() {
        // TODO reformat ids to shorts?
        return (short) m_context.getLocalId();
    }

    @Override
    public InetSocketAddress getAddress(short p_nodeID) {
        return m_context.getAddressById(p_nodeID).toInetSocketAddress();
    }

    @Override
    public List<Mapping> getAvailableMappings() {
        return null;
    }

    @Override
    public void registerListener(Listener p_listener) {

    }
}
