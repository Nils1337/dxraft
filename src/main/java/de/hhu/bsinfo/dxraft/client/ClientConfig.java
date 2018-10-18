package de.hhu.bsinfo.dxraft.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;
import de.hhu.bsinfo.dxraft.data.RaftAddress;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClientConfig {
    /**
     * List of all servers in cluster acting as raft servers
      */
    @Expose
    private List<RaftAddress> m_raftServers = new ArrayList<RaftAddress>() {
        {
            add(new RaftAddress((short) 1, "127.0.0.1", -1, 5000));
            add(new RaftAddress((short) 2, "127.0.0.1", -1,5001));
            add(new RaftAddress((short) 3, "127.0.0.1", -1,5002));
        }
    };

    /**
     * Time trying to connect to cluster before giving up (in ms)
     */
    @Expose
    private int m_overallTryDuration = 10 * 1000;

    /**
     * Timeout after getting a redirection to an unknown leader (in ms)
     */
    @Expose
    private int m_retryTimeout = 100;

    public List<RaftAddress> getRaftServers() {
        return m_raftServers;
    }

    public int getServerCount() {
        return m_raftServers.size();
    }

}
