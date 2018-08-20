package de.hhu.bsinfo.dxraft.message.server;

import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public class VoteRequest extends ServerMessage {
    private int lastLogIndex;

    public VoteRequest(int receiverId, int term, int lastLogIndex, int lastLogTerm) {
        super(receiverId, term);
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    private int lastLogTerm;

    public int getLastLogIndex() {
        return lastLogIndex;
    }

    public int getLastLogTerm() {
        return lastLogTerm;
    }

    @Override
    public void deliverMessage(ServerMessageReceiver messageReceiver) {
        messageReceiver.processVoteRequest(this);
    }
}
