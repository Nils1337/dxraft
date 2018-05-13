package de.hhu.bsinfo.dxraft.context;

import java.util.List;
import java.util.stream.Collectors;

public class RaftContext {

    // List of all servers in cluster acting as raft servers
    private List<RaftID> raftServers;

    // List of all servers in cluster acting as raft clients
    private List<RaftID> raftClients;

    private RaftID localId;

    public RaftContext(List<RaftID> raftServers, List<RaftID> raftClients, RaftID localId) {
        this.localId = localId;

        // delete local id from server list or client list
        this.raftServers = raftServers.stream().filter(id -> !id.equals(localId)).collect(Collectors.toList());
        this.raftClients = raftClients.stream().filter(id -> !id.equals(localId)).collect(Collectors.toList());
    }

    public List<RaftID> getRaftServers() {
        return raftServers;
    }

    public List<RaftID> getRaftClients() {
        return raftClients;
    }

    public RaftID getLocalId() {
        return localId;
    }
}
