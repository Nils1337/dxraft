package de.hhu.bsinfo.dxraft.context;

import java.util.List;

public class RaftContext {

    // List of all servers in cluster acting as raft servers
    private List<Short> raftServers;

    // List of all servers in cluster acting as raft clients
    private List<Short> raftClients;

    private short localId;

    public RaftContext(List<Short> raftServers, List<Short> raftClients, short localId) {
        this.raftServers = raftServers;
        this.raftClients = raftClients;
        this.localId = localId;
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
