package de.hhu.bsinfo.dxraft.net;

import de.hhu.bsinfo.dxraft.message.AppendEntriesRequest;
import de.hhu.bsinfo.dxraft.message.VoteRequest;
import de.hhu.bsinfo.dxraft.message.VoteResponse;

public interface RaftMessageReceiver {
    void processVoteRequest(VoteRequest request);
    void processVoteResponse(VoteResponse response);
    void processAppendEntriesRequest(AppendEntriesRequest request);
}
