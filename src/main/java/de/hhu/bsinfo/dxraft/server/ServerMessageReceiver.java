package de.hhu.bsinfo.dxraft.server;

import de.hhu.bsinfo.dxraft.message.client.AbstractClientRequest;
import de.hhu.bsinfo.dxraft.message.server.AppendEntriesRequest;
import de.hhu.bsinfo.dxraft.message.server.AppendEntriesResponse;
import de.hhu.bsinfo.dxraft.message.server.VoteRequest;
import de.hhu.bsinfo.dxraft.message.server.VoteResponse;

public interface ServerMessageReceiver {
    void processVoteRequest(VoteRequest p_request);
    void processVoteResponse(VoteResponse p_response);
    void processAppendEntriesRequest(AppendEntriesRequest p_request);
    void processAppendEntriesResponse(AppendEntriesResponse p_response);
    void processClientRequest(AbstractClientRequest p_request);
}
