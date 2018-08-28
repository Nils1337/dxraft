package de.hhu.bsinfo.dxraft.message.server;

import de.hhu.bsinfo.dxraft.net.RaftAddress;
import de.hhu.bsinfo.dxraft.message.RaftMessage;

public class ClientRedirection extends RaftMessage {
    private RaftAddress m_leaderAddress;

    public ClientRedirection(RaftAddress p_receiverAddress, RaftAddress p_leaderAddress) {
        super(p_receiverAddress);
        m_leaderAddress = p_leaderAddress;
    }

    public RaftAddress getLeaderAddress() {
        return m_leaderAddress;
    }
}
