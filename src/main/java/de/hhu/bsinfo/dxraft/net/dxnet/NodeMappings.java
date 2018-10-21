package de.hhu.bsinfo.dxraft.net.dxnet;

import de.hhu.bsinfo.dxnet.NodeMap;
import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.server.ServerConfig;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class NodeMappings implements NodeMap {

    private ServerConfig m_context;

    public NodeMappings(ServerConfig p_context) {
        m_context = p_context;
    }

    @Override
    public short getOwnNodeID() {
        return m_context.getLocalId();
    }

    @Override
    public InetSocketAddress getAddress(short p_nodeID) {
        return m_context.getAddressById(p_nodeID).toInetSocketAddress();
    }

    @Override
    public List<Mapping> getAvailableMappings() {
        List<Mapping> mappings = new ArrayList<>();
        for (RaftAddress address: m_context.getServers()) {
            mappings.add(new Mapping(address.getId(), address.toInetSocketAddress()));
        }
        return mappings;
    }

    @Override
    public void registerListener(Listener p_listener) {
        m_context.registerListener(p_listener);
    }
}
