package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.server.message.ServerMessage;
import de.hhu.bsinfo.dxraft.server.net.ServerMessageReceiver;

public interface VoteResponse extends ServerMessage {
    boolean isVoteGranted();

    default void deliverMessage(ServerMessageReceiver p_messageReceiver) {
        p_messageReceiver.processVoteResponse(this);
    }
}
