package de.hhu.bsinfo.dxraft.server;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private RaftAddress localAddress;

    public RaftServerContext(List<RaftAddress> raftServers, RaftAddress localAddress, int followerTimeoutDuration, int followerRandomizationAmount, int electionTimeoutDuration, int electionRandomizationAmount, int heartbeatTimeoutDuration, int heartbeatRandomizationAmount) {
        super(raftServers);
        this.localAddress = localAddress;
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

    public Set<Integer> getOtherServerIds() {
        return getRaftServers().stream().filter(address -> !address.equals(localAddress)).map(RaftAddress::getId).collect(Collectors
            .toSet());
    }

    public Set<RaftAddress> getOtherRaftServers() {
        return getRaftServers().stream().filter(address -> !address.equals(localAddress)).collect(Collectors.toSet());
    }

    public int getLocalId() {
        return localAddress.getId();
    }

    public RaftAddress getAddressById(int id) {

        if (id == localAddress.getId()) {
            return localAddress;
        }

        for (RaftAddress server : getRaftServers()) {
            if (server.getId() == id) return server;
        }
        return null;
    }

    public RaftAddress getLocalAddress() {
        return localAddress;
    }


    public Set<Integer> getServersIds() {
        return getRaftServers().stream().map(RaftAddress::getId).collect(Collectors.toSet());
    }

    public static final class RaftServerContextBuilder {
        private RaftAddress localAddress;
        private List<RaftAddress> raftServers = new ArrayList<>();
        private int followerTimeoutDuration = 100;
        private int followerRandomizationAmount = 50;
        private int electionTimeoutDuration = 100;
        private int electionRandomizationAmount = 50;
        private int heartbeatTimeoutDuration = 50;
        private int heartbeatRandomizationAmount = 0;

        private RaftServerContextBuilder() {
        }

        public static RaftServerContextBuilder aRaftServerContext() {
            return new RaftServerContextBuilder();
        }

        public RaftServerContextBuilder withRaftServers(List<RaftAddress> raftServers) {
            this.raftServers = raftServers;
            return this;
        }

        public RaftServerContextBuilder withFollowerTimeoutDuration(int followerTimeoutDuration) {
            this.followerTimeoutDuration = followerTimeoutDuration;
            return this;
        }

        public RaftServerContextBuilder withFollowerRandomizationAmount(int followerRandomizationAmount) {
            this.followerRandomizationAmount = followerRandomizationAmount;
            return this;
        }

        public RaftServerContextBuilder withLocalAddress(RaftAddress localAddress) {
            this.localAddress = localAddress;
            return this;
        }

        public RaftServerContextBuilder withElectionTimeoutDuration(int electionTimeoutDuration) {
            this.electionTimeoutDuration = electionTimeoutDuration;
            return this;
        }

        public RaftServerContextBuilder withElectionRandomizationAmount(int electionRandomizationAmount) {
            this.electionRandomizationAmount = electionRandomizationAmount;
            return this;
        }

        public RaftServerContextBuilder withHeartbeatTimeoutDuration(int heartbeatTimeoutDuration) {
            this.heartbeatTimeoutDuration = heartbeatTimeoutDuration;
            return this;
        }

        public RaftServerContextBuilder withHeartbeatRandomizationAmount(int heartbeatRandomizationAmount) {
            this.heartbeatRandomizationAmount = heartbeatRandomizationAmount;
            return this;
        }

        public RaftServerContext build() {
            if (localAddress == null) {
                throw new IllegalArgumentException("Local Address must be provided!");
            }

            // add local server to list of servers
//            if (!raftServers.contains(localAddress)) {
//                raftServers.add(localAddress);
//            }

            return new RaftServerContext(raftServers, localAddress, followerTimeoutDuration, followerRandomizationAmount, electionTimeoutDuration, electionRandomizationAmount, heartbeatTimeoutDuration, heartbeatRandomizationAmount);
        }
    }
}
