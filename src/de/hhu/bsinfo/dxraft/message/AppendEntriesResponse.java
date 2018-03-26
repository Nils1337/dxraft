package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.net.RaftMessageReceiver;

public class AppendEntriesResponse extends RaftMessage {

    private boolean success;
    private int matchIndex;

    public AppendEntriesResponse(int term, short senderId, short receiverId, boolean success, int matchIndex) {
        super(term, senderId, receiverId);
        this.success = success;
        this.matchIndex = matchIndex;
    }

    public AppendEntriesResponse(int term, short senderId, short receiverId, boolean success) {
        super(term, senderId, receiverId);
        this.success = success;
    }

    @Override
    public void processMessage(RaftMessageReceiver messageReceiver) {
        messageReceiver.processAppendEntriesResponse(this);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getMatchIndex() {
        return matchIndex;
    }
}
