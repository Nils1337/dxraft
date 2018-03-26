package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.log.LogEntry;
import de.hhu.bsinfo.dxraft.net.RaftMessageReceiver;

import java.util.List;

public class AppendEntriesRequest extends RaftMessage {

    private int prevLogIndex;
    private int prevLogTerm;
    private int leaderCommitIndex;
    private List<LogEntry> entries;

    public AppendEntriesRequest(int term, short senderId, short receiverId, int prevLogIndex, int prevLogTerm, int leaderCommitIndex, List<LogEntry> entries) {
        super(term, senderId, receiverId);
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.leaderCommitIndex = leaderCommitIndex;
        this.entries = entries;
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

    public List<LogEntry> getEntries() {
        return entries;
    }

    public int getLeaderCommitIndex() {
        return leaderCommitIndex;
    }
}
