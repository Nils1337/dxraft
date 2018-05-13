package de.hhu.bsinfo.dxraft.server;

import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.context.RaftID;

import java.util.List;

public class RaftServerContext extends RaftContext {

    // timeout duration and randomization amount when following leader
    private int followerTimeoutDuration;
    private int followerRandomizationAmount;

    // timeout duration and randomization amount when electing
    private int electionTimeoutDuration;
    private int electionRandomizationAmount;

    // timeout duration and randomization amount of leader
    private int heartbeatTimeoutDuration;
    private int heartbeatRandomizationAmount;

    public RaftServerContext(List<RaftID> raftServers, List<RaftID> raftClients, RaftID localId, int followerTimeoutDuration, int followerRandomizationAmount, int electionTimeoutDuration, int electionRandomizationAmount, int heartbeatTimeoutDuration, int heartbeatRandomizationAmount) {
        super(raftServers, raftClients, localId);
        this.followerTimeoutDuration = followerTimeoutDuration;
        this.followerRandomizationAmount = followerRandomizationAmount;
        this.electionTimeoutDuration = electionTimeoutDuration;
        this.electionRandomizationAmount = electionRandomizationAmount;
        this.heartbeatTimeoutDuration = heartbeatTimeoutDuration;
        this.heartbeatRandomizationAmount = heartbeatRandomizationAmount;
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
}
