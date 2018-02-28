package de.hhu.bsinfo.dxraft.context;

import java.util.List;

public class RaftContext {

    // List of all servers in cluster
    private List<Short> servers;

    // timeout duration and randomization amount when following leader
    private int followerTimeoutDuration;
    private int followerRandomizationAmount;

    // timeout duration and randomization amount when electing
    private int electionTimeoutDuration;
    private int electionRandomizationAmount;

    // timeout duration and randomization amount of leader
    private int heartbeatTimeoutDuration;
    private int heartbeatRandomizationAmount;

    private short localId;

    public RaftContext(List<Short> servers, int followerTimeoutDuration, int followerRandomizationAmount, int electionTimeoutDuration, int electionRandomizationAmount, int heartbeatTimeoutDuration, int heartbeatRandomizationAmount, short localId) {
        this.servers = servers;
        this.followerTimeoutDuration = followerTimeoutDuration;
        this.followerRandomizationAmount = followerRandomizationAmount;
        this.electionTimeoutDuration = electionTimeoutDuration;
        this.electionRandomizationAmount = electionRandomizationAmount;
        this.heartbeatTimeoutDuration = heartbeatTimeoutDuration;
        this.heartbeatRandomizationAmount = heartbeatRandomizationAmount;
        this.localId = localId;

        //remove localId from the list of servers
        servers.removeIf((id) -> id == localId);
    }

    public List<Short> getServers() {
        return servers;
    }

    public int getServerCount() {
        return servers.size() + 1;
    }

    public int getFollowerTimeoutDuration() {
        return followerTimeoutDuration;
    }

    public int getFollowerRandomizationAmount() {
        return followerRandomizationAmount;
    }

    public int getElectionTimeoutDuration() {
        return electionTimeoutDuration;
    }

    public int getElectionRandomizationAmount() {
        return electionRandomizationAmount;
    }

    public int getHeartbeatTimeoutDuration() {
        return heartbeatTimeoutDuration;
    }

    public int getHeartbeatRandomizationAmount() {
        return heartbeatRandomizationAmount;
    }

    public short getLocalId() {
        return localId;
    }
}
