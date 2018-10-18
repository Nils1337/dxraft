package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.log.entry.LogEntry;

import java.util.List;

public class DefaultServerMessageFactory implements ServerMessageFactory {

    @Override
    public AppendEntriesRequest newAppendEntriesRequest(short p_receiverId, int p_term, int p_prevLogIndex,
                                                        int p_prevLogTerm, int p_leaderCommitIndex,
                                                        List<LogEntry> p_entries) {
        return new DefaultAppendEntriesRequest(p_receiverId, p_term, p_prevLogIndex, p_prevLogTerm, p_leaderCommitIndex,
            p_entries);
    }

    @Override
    public AppendEntriesResponse newAppendEntriesResponse(short p_receiverId, int p_term, boolean p_success,
                                                          int p_matchIndex) {
        return new DefaultAppendEntriesResponse(p_receiverId, p_term, p_success, p_matchIndex);
    }

    @Override
    public VoteRequest newVoteRequest(short p_receiverId, int p_term, int p_lastLogIndex, int p_lastLogTerm) {
        return new DefaultVoteRequest(p_receiverId, p_term, p_lastLogIndex, p_lastLogTerm);
    }

    @Override
    public VoteResponse newVoteResponse(short p_receiverId, int p_term, boolean p_voteGranted) {
        return new DefaultVoteResponse(p_receiverId, p_term, p_voteGranted);
    }
}
