package de.hhu.bsinfo.dxraft.server;

import de.hhu.bsinfo.dxraft.message.*;

public interface ServerMessageReceiver {
    void processVoteRequest(VoteRequest request);
    void processVoteResponse(VoteResponse response);
    void processAppendEntriesRequest(AppendEntriesRequest request);
    void processAppendEntriesResponse(AppendEntriesResponse response);
    void processClientRequest(ClientRequest request);
}
