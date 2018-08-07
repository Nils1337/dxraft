package de.hhu.bsinfo.dxraft.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RaftContext {
    private static final Logger LOGGER = LogManager.getLogger();

    // List of all servers in cluster acting as raft servers
    private List<RaftAddress> raftServers;

    private RaftAddress localAddress;

    public RaftContext(List<RaftAddress> raftServers, RaftAddress localAddress) {
        this.localAddress = localAddress;
        this.raftServers = raftServers;
    }

    public Set<RaftID> getServersIds() {
        return raftServers.stream().map(RaftAddress::getId).collect(Collectors.toSet());
    }

    public List<RaftAddress> getRaftServers() {
        return raftServers;
    }

    public Set<RaftID> getOtherServerIds() {
        return raftServers.stream().filter(address -> !address.equals(localAddress)).map(RaftAddress::getId).collect(Collectors.toSet());
    }

    public Set<RaftAddress> getOtherRaftServers() {
        return raftServers.stream().filter(address -> !address.equals(localAddress)).collect(Collectors.toSet());
    }

    public RaftID getLocalId() {
        return localAddress.getId();
    }

    public RaftAddress getAddressById(RaftID id) {

        if (id.equals(localAddress.getId())) {
            return localAddress;
        }

        for (RaftAddress server : raftServers) {
            if (server.getId().equals(id)) return server;
        }
        return null;
    }

    public RaftAddress getLocalAddress() {
        return localAddress;
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
