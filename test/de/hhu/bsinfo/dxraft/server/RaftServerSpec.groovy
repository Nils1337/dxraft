package de.hhu.bsinfo.dxraft.server

import de.hhu.bsinfo.dxraft.context.RaftID
import de.hhu.bsinfo.dxraft.data.RaftData
import de.hhu.bsinfo.dxraft.message.AppendEntriesRequest
import de.hhu.bsinfo.dxraft.message.AppendEntriesResponse
import de.hhu.bsinfo.dxraft.message.ClientRedirection
import de.hhu.bsinfo.dxraft.message.ClientRequest
import de.hhu.bsinfo.dxraft.message.ClientResponse
import de.hhu.bsinfo.dxraft.message.VoteRequest
import de.hhu.bsinfo.dxraft.message.VoteResponse
import de.hhu.bsinfo.dxraft.state.Log
import de.hhu.bsinfo.dxraft.state.LogEntry
import de.hhu.bsinfo.dxraft.state.ServerState
import de.hhu.bsinfo.dxraft.state.StateMachine
import de.hhu.bsinfo.dxraft.timer.RaftTimer
import spock.lang.Shared
import spock.lang.Specification

class RaftServerSpec extends Specification {

    def context = Mock(RaftServerContext)
    def netService = Mock(ServerNetworkService)
    def log = Mock(Log)
    def timer = Mock(RaftTimer)
    def state = Mock(ServerState)

    def server = RaftServer.RaftServerBuilder
            .aRaftServer()
            .withContext(context)
            .withNetworkService(netService)
            .withLog(log)
            .withTimer(timer)
            .withState(state)
            .build()

    @Shared
    def id1 = new RaftID(1)
    @Shared
    def id2 = new RaftID(2)
    @Shared
    def id3 = new RaftID(3)

    def "test vote request"() {
        given:
            def voteRequest = Mock(VoteRequest)

        when:
            server.processVoteRequest(voteRequest)

        then:
            voteRequest.getSenderId() >> sender
            voteRequest.getTerm() >> msgTerm

            state.getVotedFor() >> votedFor
            state.getCurrentTerm() >> localTerm
            state.isFollower() >> isFollower

            log.isAtLeastAsUpToDateAs(*_) >> upd

            termUpdate * state.updateTerm(msgTerm)
            1 * netService.sendMessage({vote -> vote.voteGranted == granted})

        where:
            msgTerm | localTerm | isFollower | upd | sender | votedFor || granted
            2 | 1 | true | false | null | null || false
            2 | 1 | true | true | null | null || true
            1 | 2 | true | true | null | null || false
            4 | 2 | true | false | null | null || false
            3 | 5 | true | false | null | null || false
            1 | 1 | true | true | id1 | id2 || false
            1 | 1 | true | true | id1 | id1 || true

            termUpdate = msgTerm > localTerm ? 1 : 0
    }

    def "test vote response when not leader"() {
        given:
            def voteResponse = Mock(VoteResponse)
        when:
            server.processVoteResponse(voteResponse)

        then: "should do nothing"
            state.isCandidate() >> false
            (1.._) * state.getCurrentTerm()
            0 * state._
    }

    def "test vote response with higher term"() {
        setup:
            def voteResponse = Mock(VoteResponse)
        when:
            server.processVoteResponse(voteResponse)

        then: "should convert to follower and do nothing"
            voteResponse.getTerm() >> 3
            (1.._) * state.getCurrentTerm() >> 2
            state.isCandidate() >> false

            1 * state.updateTerm(3)
            0 * state._
    }

    def "test rejected vote"() {
        setup:
            def voteResponse = Mock(VoteResponse)

        when:
            server.processVoteResponse(voteResponse)

        then: "should update votes map but not convert state"
            state.isCandidate() >> true
            voteResponse.isVoteGranted() >> false
            voteResponse.getSenderId() >> id1

            1 * state.updateVote(id1, false)
            0 * state.convertStateToLeader()
    }

    def "test granted vote"() {
        setup:
            def voteResponse = Mock(VoteResponse)
            state.isCandidate() >> true
            voteResponse.isVoteGranted() >> true
            context.getServerCount() >> 3

        when:
            server.processVoteResponse(voteResponse)

        then: "should update votes map"
            voteResponse.getSenderId() >> id1
            state.getVotesCount() >> 1

            1 * state.updateVote(id1, true)
            0 * state.convertStateToLeader()

        when:
            server.processVoteResponse(voteResponse)

        then: "should update votes map, convert to leader and send heartbeats"
            voteResponse.getSenderId() >> id2
            state.getVotesCount() >> 2

            1 * state.updateVote(id2, true)
            1 * state.convertStateToLeader()
            1 * netService.sendMessageToAllServers({msg -> msg instanceof AppendEntriesRequest})
    }

    def "test success of append entries request handler"() {
        given:
            def request = Mock(AppendEntriesRequest)

        when:
            server.processAppendEntriesRequest(request)

        then:
            request.getTerm() >> msgTerm
            state.getCurrentTerm() >> localTerm
            log.isDiffering(_,_) >> diff

            1 * netService.sendMessage({response -> response.success == success})
            termUpdate * state.updateTerm(msgTerm)

        where:
            msgTerm | localTerm | diff || success
            1 | 2 | false || false
            2 | 1 | false || true
            1 | 2 | true || false
            2 | 1 | true || false

            termUpdate = msgTerm > localTerm ? 1 : 0
    }

//    def "test log interaction of append entries request handler"() {
//        given:
//            def request = Mock(AppendEntriesRequest)
//
//        when:
//            server.processAppendEntriesRequest(request)
//
//        then:
//            request.getTerm() >> msgTerm
//            state.getCurrentTerm() >> localTerm
//
//            1 * log.updateLog(prevIndex, entries)
//            logUpdate * log.updateCommitIndex(newCommitIndex)
//
//        where:
//            localIndex | leaderIndex | prevIndex | entries || newCommitIndex
//            0 | 0 | 0 | [new LogEntry(1)] || 0
//            0 | 1 | 0 | [new LogEntry(1)] || 1
//            1 | 5 | 2 | (1..2).collect {new LogEntry(it)} || 4
//            1 | 2 | 2 | (1..2).collect {new LogEntry(it)} || 2
//
//            logUpdate = leaderIndex > localIndex ? 1 : 0
//    }

    def "test state change of append entries request handler"() {
        given:
            def request = Mock(AppendEntriesRequest)

        when:
            server.processAppendEntriesRequest(request)

        then:
            request.getEntries() >> entries
            request.getPrevLogIndex() >> prevIndex
            request.getLeaderCommitIndex() >> leaderIndex
            log.getCommitIndex() >> localIndex

            1 * log.updateLog(prevIndex, entries)
            logUpdate * log.updateCommitIndex(newCommitIndex)

        where:
            localIndex | leaderIndex | prevIndex | entries || newCommitIndex
            0 | 0 | 0 | [new LogEntry(1)] || 0
            0 | 1 | 0 | [new LogEntry(1)] || 1
            1 | 5 | 2 | (1..2).collect {new LogEntry(it)} || 4
            1 | 2 | 2 | (1..2).collect {new LogEntry(it)} || 2

            logUpdate = leaderIndex > localIndex ? 1 : 0
    }

    def "test unsuccessful append entries request"() {
        given:
            def response = Mock(AppendEntriesResponse)
            def logEntry = Mock(LogEntry)
        when:
            server.processAppendEntriesResponse(response)

        then:
            response.isSuccess() >> false
            response.getSenderId() >> id1

            state.isLeader() >> isLeader
            log.get(_) >> logEntry
            state.getNextIndex(id1) >> 1

            doSomething * state.decrementNextIndex(id1)
            doSomething * netService.sendMessage({ request ->
                request instanceof AppendEntriesRequest &&
                        request.receiverId == id1 &&
                        request.prevLogIndex == 0})
        where:
            isLeader || doSomething
            false || 0
            true  || 1
    }

    def "test successful append entries request"() {
        given:
            def response = Mock(AppendEntriesResponse)

            def logEntries = (1..2).collect {new LogEntry(it, Mock(ClientRequest))}

            with (response) {
                isSuccess() >> true
                getSenderId() >> id1
                getMatchIndex() >> 3
            }

            with (state) {
                isLeader() >> true
                getNewCommitIndex() >> 3
            }

            with (log) {
                get(_) >> {int index -> logEntries[index-2]}
                getCommitIndex() >> 1
                getLastIndex() >> 4
            }
        when:
            server.processAppendEntriesResponse(response)

        then:
            //response should be sent for every committed entry
            2 * netService.sendMessage(_)

            //commit index should be updated correctly
            1 * log.updateCommitIndex(3)

            //index maps should be updated correctly
            1 * state.updateMatchIndex(id1, 3)
            1 * state.updateNextIndex(id1, 4)
        }

    def "test redirection"() {
        given:
            def request = Mock(ClientRequest)

        when:
            server.processClientRequest(request)

        then:
            state.isLeader() >> false
            request.getSenderId() >> id1

            1 * netService.sendMessage({response -> response instanceof ClientRedirection})
    }

    def "test read request"() {
        given:
            def request = Mock(ClientRequest)
            def stateMachine = Mock(StateMachine)
            def value = Mock(RaftData)

        when:
            server.processClientRequest(request)

        then:
            state.isLeader() >> true
            request.isReadRequest() >> true
            request.getSenderId() >> id1
            log.getStateMachine() >> stateMachine
            stateMachine.read(_) >> value

            1 * netService.sendMessage({response ->
                response instanceof ClientResponse &&
                response.value.is(value) &&
                response.receiverId == id1})
    }

    def "test write request"() {
        given:
            def request = Mock(ClientRequest)
            def servers = [id1, id2]

            state.isLeader() >> true
            request.isWriteRequest() >> true
            request.getSenderId() >> id1
            log.contains(_) >> false
            context.getOtherServerIds() >> servers

        when:
            server.processClientRequest(request)

        then:
            // should update log
            1 * log.append({logEntry -> logEntry.clientRequest.is(request)})

            // should send append entries request to each follower
            2 * netService.sendMessage({appRequest ->
                appRequest instanceof AppendEntriesRequest})
    }

    def "test duplicate write request"() {
        given:
            def request = Mock(ClientRequest)
            def prevResponse = Mock(ClientResponse)
            def logEntry = Mock(LogEntry)

        when:
            server.processClientRequest(request)

        then:
            state.isLeader() >> true
            request.isWriteRequest() >> true
            request.getRequestType() >> ClientRequest.RequestType.PUT
            log.contains(_) >> true
            log.get(_) >> logEntry
            logEntry.getClientResponse() >> prevResponse
            request.getSenderId() >> id1

            1 * netService.sendMessage({response ->
                response.is(prevResponse)})
    }
}
