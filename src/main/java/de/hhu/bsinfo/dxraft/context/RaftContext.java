package de.hhu.bsinfo.dxraft.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RaftContext {
    private static final Logger LOGGER = LogManager.getLogger();

    // List of all servers in cluster acting as raft servers
    private List<RaftAddress> raftServers;

    public RaftContext(List<RaftAddress> raftServers) {
        this.raftServers = new ArrayList<>(raftServers);
    }

    public List<RaftAddress> getRaftServers() {
        return raftServers;
    }

    public void addServer(RaftAddress newServer) {
        // TODO what if server gets added that is already in the list?
        LOGGER.info("Adding server {} to configuration", newServer.toString());
        raftServers.add(newServer);
    }

    public void removeServer(RaftAddress server) {
        LOGGER.info("Removing server {} from configuration", server.toString());
        raftServers.remove(server);
    }

    public int getServerCount() {
        return raftServers.size();
    }

}
