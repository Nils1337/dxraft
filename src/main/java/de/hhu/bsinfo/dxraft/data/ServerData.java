package de.hhu.bsinfo.dxraft.data;

import de.hhu.bsinfo.dxraft.context.RaftAddress;

public class ServerData implements RaftData {

    private RaftAddress server;

    public ServerData(RaftAddress server) {
        this.server = server;
    }

    public RaftAddress getServer() {
        return server;
    }
}
