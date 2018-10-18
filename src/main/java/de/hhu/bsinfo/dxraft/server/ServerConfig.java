package de.hhu.bsinfo.dxraft.server;

import com.google.gson.annotations.Expose;
import de.hhu.bsinfo.dxraft.data.RaftAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Data
@NoArgsConstructor
public class ServerConfig {

    private static final Logger LOGGER = LogManager.getLogger();
    // List of all servers in cluster acting as raft servers

    @Expose
    private List<RaftAddress> m_servers = new ArrayList<RaftAddress>() {
        {
            add(new RaftAddress((short) 1, "127.0.0.1", 5454, 5000));
            add(new RaftAddress((short) 2, "127.0.0.1", 5455, 5001));
            add(new RaftAddress((short) 3, "127.0.0.1", 5456,5002));
        }
    };

    // timeout duration and randomization amount when following leader
    @Expose
    private int m_followerTimeoutDuration = 100;

    @Expose
    private int m_followerRandomizationAmount = 50;

    // timeout duration and randomization amount when electing
    @Expose
    private int m_electionTimeoutDuration = 100;

    @Expose
    private int m_electionRandomizationAmount = 50;

    // timeout duration and randomization amount of leader
    @Expose
    private int m_heartbeatTimeoutDuration = 50;

    @Expose
    private int m_heartbeatRandomizationAmount = 0;

    @Expose
    private short m_localId = 1;

    @Expose
    private String m_ip = "127.0.0.1";

    @Expose
    private int m_raftPort = 5454;

    @Expose
    private int m_requestPort = 5000;

    private int m_pendingConfigChange = 0;

    public RaftAddress getRaftAddress(){
        return new RaftAddress(m_localId, m_ip, m_raftPort, m_requestPort);
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

    public Set<Short> getOtherServerIds() {
        return m_servers.stream()
            .filter(address -> !address.equals(getRaftAddress()))
            .map(RaftAddress::getId).collect(Collectors.toSet());
    }

    public Set<Short> getAllServerIds() {
        return m_servers.stream()
            .map(RaftAddress::getId).collect(Collectors.toSet());
    }

    public boolean singleServerCluster() {
        return m_servers.stream().allMatch(address -> address.equals(getRaftAddress()));
    }

    public short getLocalId() {
        return m_localId;
    }

    public RaftAddress getAddressById(int p_id) {

        if (p_id == m_localId) {
            return getRaftAddress();
        }

        for (RaftAddress server : m_servers) {
            if (server.getId() == p_id) {
                return server;
            }
        }
        return null;
    }
}
