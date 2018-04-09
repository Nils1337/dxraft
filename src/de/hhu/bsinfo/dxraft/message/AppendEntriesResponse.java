package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public class AppendEntriesResponse extends RaftServerMessage {

    private boolean success;
    private int matchIndex;

    public AppendEntriesResponse(short senderId, short receiverId, int term, boolean success) {
        super(senderId, receiverId, term);
        this.success = success;
    }

    public AppendEntriesResponse(short senderId, short receiverId, int term, boolean success, int matchIndex) {
        super(senderId, receiverId, term);
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
