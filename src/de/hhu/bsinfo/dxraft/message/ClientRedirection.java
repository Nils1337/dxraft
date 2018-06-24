package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftID;

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
