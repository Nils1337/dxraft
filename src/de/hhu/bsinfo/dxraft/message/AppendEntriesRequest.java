package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.state.LogEntry;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

import java.util.List;

public class AppendEntriesRequest extends RaftServerMessage {

    private int prevLogIndex;
    private int prevLogTerm;
    private int leaderCommitIndex;
    private List<LogEntry> entries;

    public AppendEntriesRequest(short senderId, short receiverId, int term, int prevLogIndex, int prevLogTerm, int leaderCommitIndex, List<LogEntry> entries) {
        super(senderId, receiverId, term);
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.leaderCommitIndex = leaderCommitIndex;
        this.entries = entries;
    }

    @Override
    public void deliverMessage(ServerMessageReceiver messageReceiver) {
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