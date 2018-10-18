package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.server.net.ServerMessageReceiver;

public interface AppendEntriesResponse extends ServerMessage {
    boolean isSuccess();

    int getMatchIndex();

    default void deliverMessage(ServerMessageReceiver p_messageReceiver) {
        p_messageReceiver.processAppendEntriesResponse(this);
    }
}
