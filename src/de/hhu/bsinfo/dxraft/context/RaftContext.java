package de.hhu.bsinfo.dxraft.context;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RaftContext {

    // List of all servers in cluster acting as raft servers
    private List<RaftAddress> raftServers;

    private RaftAddress localAddress;

    public RaftContext(List<RaftAddress> raftServers, RaftAddress localAddress) {
        if (localAddress == null) {
            throw new IllegalArgumentException("Local Address must not be null!");
        }
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
        return getRaftServers().stream().filter(address -> !address.equals(getLocalAddress())).map(RaftAddress::getId).collect(Collectors.toSet());
    }

    public Set<RaftAddress> getOtherRaftServers() {
        return getRaftServers().stream().filter(address -> !address.equals(getLocalAddress())).collect(Collectors.toSet());
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

    public int getServerCount() {
        return raftServers.size();
    }

}
