package de.hhu.bsinfo.dxraft.server

import de.hhu.bsinfo.dxraft.context.RaftID
import de.hhu.bsinfo.dxraft.log.Log
import de.hhu.bsinfo.dxraft.message.server.AppendEntriesRequest
import de.hhu.bsinfo.dxraft.message.server.AppendEntriesResponse
import de.hhu.bsinfo.dxraft.message.server.ClientRedirection
import de.hhu.bsinfo.dxraft.message.client.ClientRequest
import de.hhu.bsinfo.dxraft.message.server.ClientResponse
import de.hhu.bsinfo.dxraft.message.client.DeleteRequest
import de.hhu.bsinfo.dxraft.message.client.ReadRequest
import de.hhu.bsinfo.dxraft.message.server.VoteRequest
import de.hhu.bsinfo.dxraft.message.server.VoteResponse
import de.hhu.bsinfo.dxraft.message.client.WriteRequest
import de.hhu.bsinfo.dxraft.net.AbstractNetworkService

import de.hhu.bsinfo.dxraft.log.LogEntry
import de.hhu.bsinfo.dxraft.state.ServerState
import de.hhu.bsinfo.dxraft.timer.RaftTimer
import spock.lang.Shared
import spock.lang.Specification

class RaftServerSpec extends Specification {

    def context = Mock(RaftServerContext)
    def netService = Mock(AbstractNetworkService)
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

    def "test log update of append entries request handler"() {
        given:
            def request = Mock(AppendEntriesRequest)

            def requestEntries = (1..3).collect({
                Mock(LogEntry)
            })

            def committedEntries = (1..2).collect({
                Mock(LogEntry)
            })

            with (request) {
                getTerm() >> 2
                getPrevLogIndex() >> 2
                getEntries() >> requestEntries
                getLeaderCommitIndex() >> leaderIndex
            }

            state.getCurrentTerm() >> 1
            log.getCommitIndex() >> 2

        when:
            server.processAppendEntriesRequest(request)

        then:

            1 * log.updateEntries(3, requestEntries)

            // commit index should be updated correctly
            1 * log.commitEntries(newCommitIndex) >> committedEntries

        where:
            leaderIndex || newCommitIndex
            4 || 4
            5 || 5
            6 || 5
    }

    def "test log update of append entries request handler when not committing any entries"() {
        given:
            def request = Mock(AppendEntriesRequest)

            def requestEntries = (1..3).collect({
                Mock(LogEntry)
            })

            def removedEntries = (1..2).collect(){
                Mock(LogEntry)
            }

            with (request) {
                getTerm() >> 3
                getPrevLogIndex() >> 2
                getEntries() >> requestEntries
                getLeaderCommitIndex() >> 1
            }

            state.getCurrentTerm() >> 3
            log.getCommitIndex() >> 2

        when:
            server.processAppendEntriesRequest(request)

        then:
            // log should be updated correctly
            1 * log.updateEntries(3, requestEntries)

            // commit index should not be updated
            0 * log.commitEntries(_)
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
            log.getEntryByIndex(_) >> logEntry
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

            def logEntries = (1..2).collect {
                def entry = Mock(LogEntry)
                entry.buildResponse() >> Mock(ClientResponse)
                entry
            }

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
                getCommitIndex() >> 1
                getLastIndex() >> 4
            }
        when:
            server.processAppendEntriesResponse(response)

        then:
            //onCommit index should be updated correctly
            1 * log.commitEntries(3) >> logEntries

            //response should be sent for every committed entry
            2 * netService.sendMessage(_)

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
            def request = Mock(ReadRequest)
            request.isReadRequest() >> true

            def response = Mock(ClientResponse)
            state.isLeader() >> true

        when:
            server.processClientRequest(request)

        then:
            1 * request.onCommit(*_)
            1 * request.buildResponse() >> response
            1 * netService.sendMessage(response)
    }

    def "test write request"() {
        given:
            def request = Mock(WriteRequest)
            def servers = [id1, id2]
            context.getOtherServerIds() >> servers

            state.isLeader() >> true
            log.contains(_) >> false

        when:
            server.processClientRequest(request)

        then:
            // should append to log
            1 * log.append(request)

            // should send append entries request to each follower
            2 * netService.sendMessage({appRequest ->
                appRequest instanceof AppendEntriesRequest})
    }

    def "test delete request"() {
        given:
            def request = Mock(DeleteRequest)
            def servers = [id1, id2, id3]
            context.getOtherServerIds() >> servers

            state.isLeader() >> true
            log.contains(_) >> false

        when:
            server.processClientRequest(request)

        then:
            // should append to log
            1 * log.append(request)

            // should send append entries request to each follower
            3 * netService.sendMessage({appRequest ->
                appRequest instanceof AppendEntriesRequest})
    }

    def "test duplicate, committed write request"() {
        given:
            def request = Mock(WriteRequest)
            def logEntry = Mock(LogEntry)
            def response = Mock(ClientResponse)
            logEntry.isCommitted() >> true

            state.isLeader() >> true
            log.contains(_) >> true
            log.getEntryByIndex(_) >> logEntry

        when:
            server.processClientRequest(request)

        then: "should send response because entry is already committed"
            1 * logEntry.updateClientRequest(request)
            1 * logEntry.buildResponse() >> response
            1 * netService.sendMessage(response)
    }

    def "test duplicate, uncommitted write request"() {
        given:
            def request = Mock(WriteRequest)
            def logEntry = Mock(LogEntry)
            def response = Mock(ClientResponse)
            logEntry.isCommitted() >> false

            state.isLeader() >> true
            log.contains(_) >> true
            log.getEntryByIndex(_) >> logEntry

        when:
            server.processClientRequest(request)

        then: "should do nothing because entry is not committed"
            1 * logEntry.updateClientRequest(request)
            0 * netService.sendMessage(response)
    }

    def "test add server request"() {

    }
}
