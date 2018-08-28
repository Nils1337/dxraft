package de.hhu.bsinfo.dxraft.server;

import de.hhu.bsinfo.dxraft.net.RaftAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerContext {

    private static final Logger LOGGER = LogManager.getLogger();
    // List of all servers in cluster acting as raft servers
    private List<RaftAddress> m_servers;

    // timeout duration and randomization amount when following leader
    private int m_followerTimeoutDuration;
    private int m_followerRandomizationAmount;

    // timeout duration and randomization amount when electing
    private int m_electionTimeoutDuration;
    private int m_electionRandomizationAmount;

    // timeout duration and randomization amount of leader
    private int m_heartbeatTimeoutDuration;
    private int m_heartbeatRandomizationAmount;

    private int m_pendingConfigChange = 0;

    private RaftAddress m_localAddress;

    public ServerContext(List<RaftAddress> p_servers, RaftAddress p_localAddress, int p_followerTimeoutDuration,
        int p_followerRandomizationAmount, int p_electionTimeoutDuration, int p_electionRandomizationAmount,
        int p_heartbeatTimeoutDuration, int p_heartbeatRandomizationAmount) {
        m_servers = new ArrayList<>(p_servers);
        m_localAddress = p_localAddress;
        m_followerTimeoutDuration = p_followerTimeoutDuration;
        m_followerRandomizationAmount = p_followerRandomizationAmount;
        m_electionTimeoutDuration = p_electionTimeoutDuration;
        m_electionRandomizationAmount = p_electionRandomizationAmount;
        m_heartbeatTimeoutDuration = p_heartbeatTimeoutDuration;
        m_heartbeatRandomizationAmount = p_heartbeatRandomizationAmount;
    }

    public int getFollowerTimeoutDuration() {
        return m_followerTimeoutDuration;
    }

    public int getFollowerRandomizationAmount() {
        return m_followerRandomizationAmount;
    }

    public int getElectionTimeoutDuration() {
        return m_electionTimeoutDuration;
    }

    public int getElectionRandomizationAmount() {
        return m_electionRandomizationAmount;
    }

    public int getHeartbeatTimeoutDuration() {
        return m_heartbeatTimeoutDuration;
    }

    public int getHeartbeatRandomizationAmount() {
        return m_heartbeatRandomizationAmount;
    }

    public List<RaftAddress> getServers() {
        return m_servers;
    }

    public void setLocalAddress(RaftAddress p_localAddress) {
        m_localAddress = p_localAddress;
    }

    public int getServerCount() {
        return m_servers.size() - m_pendingConfigChange;
    }

    public void startAddServer(RaftAddress p_newServer) {
        // TODO what if server gets added that is already in the list?
        if (m_pendingConfigChange == 0) {
            LOGGER.info("Started adding server {} to configuration", p_newServer.toString());
            m_pendingConfigChange = 1;
            m_servers.add(p_newServer);
        } else {
            throw new IllegalStateException("Only one concurrent config change allowed!");
        }
    }

    public void cancelAddServer(RaftAddress p_newServer) {
        if (m_pendingConfigChange == 1) {
            LOGGER.info("Cancelled adding server {} to configuration", p_newServer.toString());
            m_pendingConfigChange = 0;
            m_servers.remove(p_newServer);
        } else {
            throw new IllegalStateException("No config change pending");
        }
    }

    public void finishAddServer(RaftAddress p_newServer) {
        LOGGER.info("Finished adding server {} to configuration", p_newServer.toString());
        m_pendingConfigChange = 0;
    }

    public void startRemoveServer(RaftAddress p_server) {
        if (m_pendingConfigChange == 0) {
            LOGGER.info("Started removing server {} from configuration", p_server.toString());
            m_pendingConfigChange = 1;
        } else {
            throw new IllegalStateException("Only one concurrent config change allowed!");
        }
    }

    public void cancelRemoveServer(RaftAddress p_newServer) {
        if (m_pendingConfigChange == 1) {
            LOGGER.info("Cancelled removing server {} from configuration", p_newServer.toString());
            m_pendingConfigChange = 0;
        } else {
            throw new IllegalStateException("No config change pending");
        }
    }

    public void finishRemoveServer(RaftAddress p_server) {
        LOGGER.info("Finished removing server {} from configuration", p_server.toString());
        m_pendingConfigChange = 0;
        m_servers.remove(p_server);
    }

    public Set<Integer> getOtherServerIds() {
        return m_servers.stream()
            .filter(address -> !address.equals(m_localAddress))
            .map(RaftAddress::getId).collect(Collectors.toSet());
    }

    public boolean singleServerCluster() {
        return m_servers.stream().allMatch(address -> address.equals(m_localAddress));
    }

    public int getLocalId() {
        return m_localAddress.getId();
    }

    public RaftAddress getAddressById(int p_id) {

        if (p_id == m_localAddress.getId()) {
            return m_localAddress;
        }

        for (RaftAddress server : m_servers) {
            if (server.getId() == p_id) {
                return server;
            }
        }
        return null;
    }

    public RaftAddress getLocalAddress() {
        return m_localAddress;
    }

    public static final class RaftServerContextBuilder {
        private RaftAddress m_localAddress;
        private List<RaftAddress> m_raftServers = new ArrayList<>();
        private int m_followerTimeoutDuration = 100;
        private int m_followerRandomizationAmount = 50;
        private int m_electionTimeoutDuration = 100;
        private int m_electionRandomizationAmount = 50;
        private int m_heartbeatTimeoutDuration = 50;
        private int m_heartbeatRandomizationAmount = 0;

        private RaftServerContextBuilder() {
        }

        public static RaftServerContextBuilder aRaftServerContext() {
            return new RaftServerContextBuilder();
        }

        public RaftServerContextBuilder withRaftServers(List<RaftAddress> p_raftServers) {
            m_raftServers = p_raftServers;
            return this;
        }

        public RaftServerContextBuilder withFollowerTimeoutDuration(int p_followerTimeoutDuration) {
            m_followerTimeoutDuration = p_followerTimeoutDuration;
            return this;
        }

        public RaftServerContextBuilder withFollowerRandomizationAmount(int p_followerRandomizationAmount) {
            m_followerRandomizationAmount = p_followerRandomizationAmount;
            return this;
        }

        public RaftServerContextBuilder withLocalAddress(RaftAddress p_localAddress) {
            m_localAddress = p_localAddress;
            return this;
        }

        public RaftServerContextBuilder withElectionTimeoutDuration(int p_electionTimeoutDuration) {
            m_electionTimeoutDuration = p_electionTimeoutDuration;
            return this;
        }

        public RaftServerContextBuilder withElectionRandomizationAmount(int p_electionRandomizationAmount) {
            m_electionRandomizationAmount = p_electionRandomizationAmount;
            return this;
        }

        public RaftServerContextBuilder withHeartbeatTimeoutDuration(int p_heartbeatTimeoutDuration) {
            m_heartbeatTimeoutDuration = p_heartbeatTimeoutDuration;
            return this;
        }

        public RaftServerContextBuilder withHeartbeatRandomizationAmount(int p_heartbeatRandomizationAmount) {
            m_heartbeatRandomizationAmount = p_heartbeatRandomizationAmount;
            return this;
        }

        public ServerContext build() {
            if (m_localAddress == null) {
                throw new IllegalArgumentException("Local Address must be provided!");
            }

            // add local server to list of servers
//            if (!raftServers.contains(localAddress)) {
//                raftServers.add(localAddress);
//            }

            return new ServerContext(m_raftServers, m_localAddress, m_followerTimeoutDuration,
                m_followerRandomizationAmount, m_electionTimeoutDuration, m_electionRandomizationAmount,
                m_heartbeatTimeoutDuration, m_heartbeatRandomizationAmount);
        }
    }
}
