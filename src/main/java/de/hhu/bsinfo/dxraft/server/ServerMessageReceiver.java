package de.hhu.bsinfo.dxraft.server;

import de.hhu.bsinfo.dxraft.message.client.AddServerRequest;
import de.hhu.bsinfo.dxraft.message.client.ClientRequest;
import de.hhu.bsinfo.dxraft.message.client.RemoveServerRequest;
import de.hhu.bsinfo.dxraft.message.server.AppendEntriesRequest;
import de.hhu.bsinfo.dxraft.message.server.AppendEntriesResponse;
import de.hhu.bsinfo.dxraft.message.server.VoteRequest;
import de.hhu.bsinfo.dxraft.message.server.VoteResponse;

public interface ServerMessageReceiver {
    void processVoteRequest(VoteRequest request);
    void processVoteResponse(VoteResponse response);
    void processAppendEntriesRequest(AppendEntriesRequest request);
    void processAppendEntriesResponse(AppendEntriesResponse response);
    void processClientRequest(ClientRequest request);
}
