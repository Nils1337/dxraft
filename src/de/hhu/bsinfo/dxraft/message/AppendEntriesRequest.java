package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.log.LogEntry;
import de.hhu.bsinfo.dxraft.net.RaftMessageReceiver;

public class AppendEntriesRequest extends RaftMessage {

    private int prevLogIndex;
    private int prevLogTerm;
    private int leaderCommitIndex;
    private LogEntry[] entries;

    public AppendEntriesRequest(int term, short senderId, short receiverId) {
        super(term, senderId, receiverId);
    }

    @Override
    public void processMessage(RaftMessageReceiver messageReceiver) {
        messageReceiver.processAppendEntriesRequest(this);
    }

    public int getPrevLogIndex() {
        return prevLogIndex;
    }

    public int getPrevLogTerm() {
        return prevLogTerm;
    }

    public LogEntry[] getEntries() {
        return entries;
    }

    public int getLeaderCommitIndex() {
        return leaderCommitIndex;
    }
}
