package de.hhu.bsinfo.dxraft.state

import de.hhu.bsinfo.dxraft.context.RaftID
import de.hhu.bsinfo.dxraft.log.Log
import de.hhu.bsinfo.dxraft.server.RaftServerContext
import de.hhu.bsinfo.dxraft.timer.RaftTimer
import spock.lang.Specification
import spock.lang.Unroll


class ServerStateSpec extends Specification {

    def timer = Mock(RaftTimer)
    def log = Mock(Log)
    def context = Mock(RaftServerContext)
    def state = new ServerState(context)
    def id1 = new RaftID(1)
    def id2 = new RaftID(2)
    def id3 = new RaftID(3)
    def servers = [id2, id3]

    def setup() {
        context.getOtherServerIds() >> servers
        state.setTimer(timer)
        state.setLog(log)
        state.becomeActive()
    }

    def "test state changes"() {
        expect: "initial state"
            state.isFollower()
            state.getCurrentTerm() == 0
        when: "timeout => convert to candidate"
            state.convertStateToCandidate()
        then:
            1 * timer.reset(ServerState.State.CANDIDATE)
            context.getLocalId() >> id1

            state.getCurrentTerm() == 1
            state.isCandidate()
            state.getVotedFor().is(id1)

        when: "election timed out => reset candidate"
            state.resetStateAsCandidate()
        then:
            1 * timer.reset(ServerState.State.CANDIDATE)
            context.getLocalId() >> id1

            state.isCandidate()
            state.getCurrentTerm() == 2
            state.getVotedFor().is(id1)

        when: "election successful => convert to leader"
            state.convertStateToLeader()
        then:
            1 * timer.reset(ServerState.State.LEADER)
            context.getServersIds() >> servers
            log.getUncommittedEntries() >> []

            state.isLeader()
            state.getCurrentTerm() == 2
            servers.each { id ->
                state.getMatchIndex(id) == 0
                state.getNextIndex(id) == 4
            }

        when: "timeout => reset timer"
            state.resetStateAsLeader()
        then:
            1 * timer.reset(ServerState.State.LEADER)

        when: "new leader was elected => convert to follower"
            state.convertStateToFollower()
        then:
            1 * timer.reset(ServerState.State.FOLLOWER)
            state.isFollower()
            state.getVotedFor() == null
    }

    def "test updating term"() {
        setup:
            state.convertStateToCandidate()
        when:
            state.updateTerm(2)
        then:
            1 * timer.reset(ServerState.State.FOLLOWER)
            state.isFollower()
            state.getCurrentTerm() == 2
            state.getVotedFor() == null

        when:
            state.updateTerm(3)
        then:
            1 * timer.reset(ServerState.State.FOLLOWER)
            state.isFollower()
            state.getCurrentTerm() == 3
            state.getVotedFor() == null
    }


    @Unroll
    def "test calculation of new commit index"() {
        setup:
            state.updateTerm(currentTerm)
            state.setState(ServerState.State.LEADER)

            state.updateMatchIndex(id1, index1)
            state.updateMatchIndex(id2, index2)

            context.getServerCount() >> 5
            log.getCommitIndex() >> -1
            log.getLastIndex() >> 5
            log.getTermByIndex(_) >> {int index -> index}

        expect:
            state.getNewCommitIndex() == newCommitIndex
        where:
            index1 | index2 | currentTerm || newCommitIndex
            0 | 0 | 0 || 0
            1 | 2 | 1 || 1
            3 | 3 | 1 || 1
            3 | 3 | 3 || 3
            5 | 3 | 2 || 2
    }
}