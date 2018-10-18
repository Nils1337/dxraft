package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.log.entry.LogEntry;
import de.hhu.bsinfo.dxraft.server.net.ServerMessageReceiver;

import java.util.List;

public interface AppendEntriesRequest extends ServerMessage {
    int getPrevLogIndex();

    int getPrevLogTerm();

    List<LogEntry> getEntries();

    int getLeaderCommitIndex();

    default void deliverMessage(ServerMessageReceiver p_messageReceiver) {
        p_messageReceiver.processAppendEntriesRequest(this);
    }
}
