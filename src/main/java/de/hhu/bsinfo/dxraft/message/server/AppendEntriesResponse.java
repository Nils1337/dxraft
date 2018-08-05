package de.hhu.bsinfo.dxraft.message.server;

import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public class AppendEntriesResponse extends ServerMessage {

    private boolean success;
    private int matchIndex;

    public AppendEntriesResponse(RaftID receiverId, int term, boolean success) {
        super(receiverId, term);
        this.success = success;
    }

    public AppendEntriesResponse(RaftID senderId, RaftID receiverId, int term, boolean success, int matchIndex) {
        super(receiverId, term);
        this.success = success;
        this.matchIndex = matchIndex;
    }

    @Override
    public void deliverMessage(ServerMessageReceiver messageReceiver) {
        messageReceiver.processAppendEntriesResponse(this);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getMatchIndex() {
        return matchIndex;
    }
}
