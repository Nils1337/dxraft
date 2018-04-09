package de.hhu.bsinfo.dxraft.context;

import java.util.List;
import java.util.stream.Collectors;

public class RaftContext {

    // List of all servers in cluster acting as raft servers
    private List<Short> raftServers;

    // List of all servers in cluster acting as raft clients
    private List<Short> raftClients;

    private short localId;

    public RaftContext(List<Short> raftServers, List<Short> raftClients, short localId) {
        this.localId = localId;

        // delete local id from server list or client list
        this.raftServers = raftServers.stream().filter(id -> id != localId).collect(Collectors.toList());
        this.raftClients = raftClients.stream().filter(id -> id != localId).collect(Collectors.toList());
    }

    public List<Short> getRaftServers() {
        return raftServers;
    }

    public List<Short> getRaftClients() {
        return raftClients;
    }

    public short getLocalId() {
        return localId;
    }
}
