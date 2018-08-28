package de.hhu.bsinfo.dxraft.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import de.hhu.bsinfo.dxraft.net.RaftAddress;

public class ClientContext {
    // List of all servers in cluster acting as raft servers
    private List<RaftAddress> m_raftServers;

    public ClientContext(List<RaftAddress> p_raftServers) {
        m_raftServers = new ArrayList<>(p_raftServers);
    }

    public List<RaftAddress> getRaftServers() {
        return m_raftServers;
    }

    public int getServerCount() {
        return m_raftServers.size();
    }

}
