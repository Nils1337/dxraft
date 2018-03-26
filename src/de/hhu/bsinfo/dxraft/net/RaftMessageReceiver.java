package de.hhu.bsinfo.dxraft.net;

import de.hhu.bsinfo.dxraft.message.*;

public interface RaftMessageReceiver {
    void processMessage(RaftMessage message);
    void processVoteRequest(VoteRequest request);
    void processVoteResponse(VoteResponse response);
    void processAppendEntriesRequest(AppendEntriesRequest request);
    void processAppendEntriesResponse(AppendEntriesResponse response);
}
