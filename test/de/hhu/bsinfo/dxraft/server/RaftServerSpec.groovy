package de.hhu.bsinfo.dxraft.server

import de.hhu.bsinfo.dxraft.context.RaftID
import de.hhu.bsinfo.dxraft.data.RaftData
import de.hhu.bsinfo.dxraft.message.AppendEntriesRequest
import de.hhu.bsinfo.dxraft.message.AppendEntriesResponse
import de.hhu.bsinfo.dxraft.message.ClientRedirection
import de.hhu.bsinfo.dxraft.message.ClientRequest
import de.hhu.bsinfo.dxraft.message.ClientResponse
import de.hhu.bsinfo.dxraft.message.RaftMessage
import de.hhu.bsinfo.dxraft.message.VoteRequest
import de.hhu.bsinfo.dxraft.message.VoteResponse
import de.hhu.bsinfo.dxraft.state.Log
import de.hhu.bsinfo.dxraft.state.LogEntry
import de.hhu.bsinfo.dxraft.state.StateMachine
import de.hhu.bsinfo.dxraft.timer.RaftTimer
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

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

    def "test processVoteRequest"() {
        given:
            def voteRequest = Mock(VoteRequest)

        when:
            server.processVoteRequest(voteRequest)

        then:
            voteRequest.getSenderId() >> sender
            voteRequest.getTerm() >> msgTerm

            state.getVotedFor() >> votedFor
            state.getCurrentTerm() >> localTerm

            log.isAtLeastAsUpToDateAs(_,_) >> upd

            termUpdate * state.updateTerm(msgTerm)
            1 * netService.sendMessage({vote -> vote.voteGranted == granted})

        where:
            msgTerm | localTerm | upd | sender | votedFor || granted
            2 | 1 | true | null | null || true
            1 | 2 | true | null | null || false
            4 | 2 | false | null | null || false
            3 | 5 | false | null | null || false
            1 | 1 | true | id1 | id2 || false
            1 | 1 | true | id1 | id1 || true

            termUpdate = msgTerm > localTerm ? 1 : 0
    }

    def "test processVoteResponse"() {
        given:
            def voteResponse = Mock(VoteResponse)
            def servers = Mock(List)

        when:
            server.processVoteResponse(voteResponse)

        then:
            context.getRaftServers() >> servers
            servers.size() >> serverCount

            voteResponse.getSenderId() >> sender
            voteResponse.getTerm() >> msgTerm
            voteResponse.isVoteGranted() >> granted

            state.getCurrentTerm() >> localTerm
            state.isCandidate() >> isCandidate
            state.getVotesMap() >> votes

            votes == votesAfter
            termUpdate * state.updateTerm(msgTerm)
            leader * state.convertStateToLeader();

        where:
            isCandidate | msgTerm | localTerm | granted | sender | votes | serverCount || votesAfter | leader
            false | 2 | 1 | true | null | [:] | 0 || [:] | 0
            false | 1 | 2 | true | null | [:] | 0 || [:] | 0
            false | 1 | 1 | false | null | [:] | 0 || [:] | 0
            true | 1 | 2 | false | id1 | [:] | 3 || [(id1): false] | 0
            true | 3 | 2 | true | id1 | [:] | 3 || [(id1): true]| 0
            true | 2 | 1 | true | id1 | [(id2): false] | 3 || [(id1): true, (id2): false] | 0
            true | 1 | 1 | true | id1 | [(id2): true] | 3 || [(id1): true, (id2): true] | 1

            termUpdate = msgTerm > localTerm ? 1 : 0

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

    def "test log interaction of append entries request handler"() {
        given:
            def request = Mock(AppendEntriesRequest)

        when:
            server.processAppendEntriesRequest(request)

        then:
            request.getTerm() >> msgTerm
            state.getCurrentTerm() >> localTerm

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
            state.getNextIndexMap() >> nextIndexMap
            log.get(_) >> logEntry

            request * netService.sendMessage({ request ->
                request instanceof AppendEntriesRequest &&
                        request.receiverId == id1 &&
                        request.prevLogIndex == 0})

            nextIndexMap == nextIndexMapAfter

        where:

            isLeader | nextIndexMap || request | nextIndexMapAfter
            false | [:] || 0 | [:]
            true | [(id1): 2] || 1 | [(id1): 1]
    }

    def "test successful append entries request"() {
        given:
            def response = Mock(AppendEntriesResponse)

            def logEntries = (1..2).collect {new LogEntry(it, new ClientRequest(new RaftID(it), new RaftID(0), ClientRequest.RequestType.GET, null, null))}

            def nextIndexMap = [(id1): 0, (id2): 4, (id3): 2]
            def matchIndexMap = [(id1): 0, (id2): 3, (id3): 1]

        when:
            server.processAppendEntriesResponse(response)

        then:
            response.isSuccess() >> true
            response.getSenderId() >> id1
            response.getMatchIndex() >> 3

            state.isLeader() >> true
            state.getNextIndexMap() >> nextIndexMap
            state.getMatchIndexMap() >> matchIndexMap
            log.get(_) >> {int index -> logEntries[index-2]}
            log.getCommitIndex() >> 1
            log.getLastIndex() >> 4
            log.getNewCommitIndex(*_) >> 3

            //response should be sent for every committed entry
            1 * netService.sendMessage({ it.receiverId == new RaftID(1)})
            1 * netService.sendMessage({ it.receiverId == new RaftID(2)})

            //commit index should be updated correctly
            1 * log.updateCommitIndex(3)

            //index maps should be updated correctly
            matchIndexMap == [(id1): 3, (id2): 3, (id3): 1]
            nextIndexMap == [(id1): 4, (id2): 4, (id3): 2]
        }

    def "test redirection"() {
        given:
            def request = Mock(ClientRequest)

        when:
            server.processClientRequest(request)

        then:
            state.isLeader() >> false
            request.getSenderId() >> id1

            1 * netService.sendMessage({response ->
                response instanceof ClientRedirection &&
                        response.receiverId == id1})
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
