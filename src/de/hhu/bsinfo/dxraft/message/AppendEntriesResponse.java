package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.net.RaftMessageReceiver;

public class AppendEntriesResponse extends RaftMessage {

    private boolean success;

    public AppendEntriesResponse(int term, short senderId, short receiverId, boolean success) {
        super(term, senderId, receiverId);
        this.success = success;
    }

    @Override
    public void processMessage(RaftMessageReceiver messageReceiver) {
        //messageReceiver.
    }

    public boolean isSuccess() {
        return success;
    }
}
