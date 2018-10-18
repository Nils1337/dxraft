package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.server.net.ServerMessageReceiver;

public interface VoteRequest extends ServerMessage {
    int getLastLogIndex();
    int getLastLogTerm();

    default void deliverMessage(ServerMessageReceiver p_messageReceiver) {
        p_messageReceiver.processVoteRequest(this);
    }
}
