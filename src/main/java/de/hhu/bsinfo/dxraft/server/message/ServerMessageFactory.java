package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.log.entry.LogEntry;

import java.util.List;

public interface ServerMessageFactory {
    AppendEntriesRequest newAppendEntriesRequest(short p_receiverId, int p_term, int p_lastLogIndex, int p_lastLogTerm,
                                                 int p_commitIndex, List<LogEntry> p_entries);
    AppendEntriesResponse newAppendEntriesResponse(short p_receiverId, int p_term, boolean p_success, int p_matchIndex);
    VoteRequest newVoteRequest(short p_receiverId, int p_term, int p_lastLogIndex, int p_lastLogTerm);
    VoteResponse newVoteResponse(short p_receiverId, int p_term, boolean p_voteGranted);
}
