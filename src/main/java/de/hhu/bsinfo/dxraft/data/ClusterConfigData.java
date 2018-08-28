package de.hhu.bsinfo.dxraft.data;

import de.hhu.bsinfo.dxraft.net.RaftAddress;

import java.util.List;

public class ClusterConfigData implements RaftData {

    private List<RaftAddress> m_servers;

    public ClusterConfigData(List<RaftAddress> p_servers) {
        m_servers = p_servers;
    }

    public List<RaftAddress> getServers() {
        return m_servers;
    }

}
