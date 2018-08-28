package de.hhu.bsinfo.dxraft.data;

import de.hhu.bsinfo.dxraft.net.RaftAddress;

public class ServerData implements RaftData {

    private RaftAddress m_server;

    public ServerData(RaftAddress p_server) {
        m_server = p_server;
    }

    public RaftAddress getServer() {
        return m_server;
    }
}
