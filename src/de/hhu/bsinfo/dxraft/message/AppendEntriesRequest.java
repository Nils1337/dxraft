package de.hhu.bsinfo.dxraft.message;

        import de.hhu.bsinfo.dxraft.RaftServer;
        import de.hhu.bsinfo.dxraft.net.RaftMessageReceiver;

public class AppendEntriesRequest extends RaftMessage {

    public AppendEntriesRequest(int term, short senderId, short receiverId) {
        super(term, senderId, receiverId);
    }

    @Override
    public void processMessage(RaftMessageReceiver messageReceiver) {
        messageReceiver.processAppendEntriesRequest(this);
    }

}
