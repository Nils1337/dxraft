package de.hhu.bsinfo.dxraft.server

import de.hhu.bsinfo.dxraft.context.RaftID
import de.hhu.bsinfo.dxraft.state.Log
import de.hhu.bsinfo.dxraft.timer.RaftTimer
import spock.lang.Specification


class ServerStateSpec extends Specification {

    def timer = Mock(RaftTimer)
    def log = Mock(Log)
    def context = Mock(RaftServerContext)
    def state = new ServerState(context, timer, log)
    def id1 = new RaftID(1)
    def id2 = new RaftID(2)
    def id3 = new RaftID(3)
    def servers = [id1, id2, id3]

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
            context.getRaftServers() >> servers

            state.isLeader()
            state.getCurrentTerm() == 2
            servers.each { id ->
                state.getMatchIndexMap()[(id)] == 0
                state.getNextIndexMap()[(id)] == 4
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
}