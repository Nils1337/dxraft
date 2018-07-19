package de.hhu.bsinfo.dxraft.data;

import de.hhu.bsinfo.dxraft.context.RaftAddress;

import java.util.List;

public class ClusterConfigData implements RaftData {

    private List<RaftAddress> servers;

    public ClusterConfigData(List<RaftAddress> servers) {
        this.servers = servers;
    }

    public List<RaftAddress> getServers() {
        return servers;
    }

}
