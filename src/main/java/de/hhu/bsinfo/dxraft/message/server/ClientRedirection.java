package de.hhu.bsinfo.dxraft.message.server;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.message.RaftMessage;

public class ClientRedirection extends RaftMessage {
    private RaftAddress leaderAddress;

    public ClientRedirection(RaftAddress receiverAddress, RaftAddress leaderAddress) {
        super(receiverAddress);
        this.leaderAddress = leaderAddress;
    }

    public RaftAddress getLeaderAddress() {
        return leaderAddress;
    }
}
