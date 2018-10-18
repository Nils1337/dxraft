package de.hhu.bsinfo.dxraft.server.net;

import de.hhu.bsinfo.dxraft.server.message.VoteRequest;
import de.hhu.bsinfo.dxraft.server.message.VoteResponse;
import de.hhu.bsinfo.dxraft.server.message.*;

public interface ServerMessageReceiver {
    void processVoteRequest(VoteRequest p_request);
    void processVoteResponse(VoteResponse p_response);
    void processAppendEntriesRequest(AppendEntriesRequest p_request);
    void processAppendEntriesResponse(AppendEntriesResponse p_response);
}
