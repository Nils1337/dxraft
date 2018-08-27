package de.hhu.bsinfo.dxraft.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RaftContext {
    private static final Logger LOGGER = LogManager.getLogger();

    // List of all servers in cluster acting as raft servers
    private List<RaftAddress> m_raftServers;

    public RaftContext(List<RaftAddress> p_raftServers) {
        this.m_raftServers = new ArrayList<>(p_raftServers);
    }

    public List<RaftAddress> getRaftServers() {
        return m_raftServers;
    }

    public void addServer(RaftAddress p_newServer) {
        // TODO what if server gets added that is already in the list?
        LOGGER.info("Adding server {} to configuration", p_newServer.toString());
        m_raftServers.add(p_newServer);
    }

    public void removeServer(RaftAddress p_server) {
        LOGGER.info("Removing server {} from configuration", p_server.toString());
        m_raftServers.remove(p_server);
    }

    public int getServerCount() {
        return m_raftServers.size();
    }

}
