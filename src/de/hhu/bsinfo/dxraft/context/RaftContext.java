package de.hhu.bsinfo.dxraft.context;

import de.hhu.bsinfo.dxraft.server.RaftServer;

import java.util.List;
import java.util.stream.Collectors;

public class RaftContext {

    // List of all servers in cluster acting as raft servers
    private List<RaftAddress> raftServers;

    // List of all servers in cluster acting as raft clients
    private List<RaftAddress> raftClients;

    private RaftAddress localAddress;

    public RaftContext(List<RaftAddress> raftServers, List<RaftAddress> raftClients, RaftAddress localAddress) {
        this.localAddress = localAddress;

        // delete local id from server list or client list
        this.raftServers = raftServers.stream().filter(addr -> !addr.equals(localAddress)).collect(Collectors.toList());
        this.raftClients = raftClients.stream().filter(addr-> !addr.equals(localAddress)).collect(Collectors.toList());
    }

    public List<RaftID> getRaftServers() {
        return raftServers.stream().map(RaftAddress::getId).collect(Collectors.toList());
    }

    public List<RaftID> getRaftClients() {
        return raftClients.stream().map(RaftAddress::getId).collect(Collectors.toList());
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
        for (RaftAddress server : raftClients) {
            if (server.getId().equals(id)) return server;
        }
        return null;
    }

    public RaftAddress getLocalAddress() {
        return localAddress;
    }
}
