package de.hhu.bsinfo.dxraft.server

import de.hhu.bsinfo.dxraft.client.message.Request
import de.hhu.bsinfo.dxraft.data.RaftAddress

import de.hhu.bsinfo.dxraft.log.Log
import de.hhu.bsinfo.dxraft.log.LogEntryFactory
import de.hhu.bsinfo.dxraft.server.message.AppendEntriesRequest
import de.hhu.bsinfo.dxraft.server.message.AppendEntriesResponse
import de.hhu.bsinfo.dxraft.server.message.DefaultAppendEntriesRequest
import de.hhu.bsinfo.dxraft.server.message.DefaultAppendEntriesResponse


import de.hhu.bsinfo.dxraft.server.message.DefaultVoteRequest
import de.hhu.bsinfo.dxraft.server.message.DefaultVoteResponse

import de.hhu.bsinfo.dxraft.log.entry.LogEntry
import de.hhu.bsinfo.dxraft.server.message.RequestResponse
import de.hhu.bsinfo.dxraft.server.message.ResponseMessageFactory
import de.hhu.bsinfo.dxraft.server.message.ServerMessageFactory
import de.hhu.bsinfo.dxraft.server.message.VoteRequest
import de.hhu.bsinfo.dxraft.server.message.VoteResponse
import de.hhu.bsinfo.dxraft.server.net.AbstractRequestNetworkService
import de.hhu.bsinfo.dxraft.server.net.AbstractServerNetworkService
import de.hhu.bsinfo.dxraft.state.ServerState
import de.hhu.bsinfo.dxraft.timer.RaftTimer
import spock.lang.Specification
import spock.lang.Unroll

class RaftServerSpec extends Specification {

    def context = Mock(ServerConfig)
    def netService = Mock(AbstractServerNetworkService)
    def requestNetService = Mock(AbstractRequestNetworkService)
    def responseFactory = Mock(ResponseMessageFactory)
    def serverMessageFactory = Mock(ServerMessageFactory)
    def logEntryFactory = Mock(LogEntryFactory)
    def log = Mock(Log)
    def timer = Mock(RaftTimer)
    def state = Mock(ServerState)

    def server = RaftServer.RaftServerBuilder
            .aRaftServer()
            .withContext(context)
            .withNetworkService(netService)
            .withRequestNetworkService(requestNetService)
            .withResponseMessageFactory(responseFactory)
            .withServerMessageFactory(serverMessageFactory)
            .withLogEntryFactory(logEntryFactory)
            .withLog(log)
            .withTimer(timer)
            .withState(state)
            .build()

    def setup() {
        context.getLocalId() << 1
    }

    @Unroll
    def "test vote request"() {
        given:
            def voteRequest = Mock(VoteRequest)
            def response = Mock(VoteResponse)

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
            1 * serverMessageFactory.newVoteResponse(sender, localTerm, granted) >> response
            1 * netService.sendMessage(response)

        where:
            msgTerm | localTerm | upd | votedFor || granted
            2 | 1 | false | RaftAddress.INVALID_ID || false
            2 | 1 | true | RaftAddress.INVALID_ID || true
            1 | 2 | true  | RaftAddress.INVALID_ID || false
            4 | 2 | false | RaftAddress.INVALID_ID || false
            3 | 5 | false | RaftAddress.INVALID_ID || false
            1 | 1 | true | 2 || false
            1 | 1 | true | 1 || true

            isFollower = true
            sender = 1
            termUpdate = msgTerm > localTerm ? 1 : 0
    }

    def "test vote response when not leader"() {
        given:
            def voteResponse = Mock(DefaultVoteResponse)
        when:
            server.processVoteResponse(voteResponse)

        then: "should do nothing"
            state.isCandidate() >> false
            (1.._) * state.getCurrentTerm()
            0 * state._
    }

    def "test vote response with higher term"() {
        setup:
            def voteResponse = Mock(DefaultVoteResponse)
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
            def voteResponse = Mock(DefaultVoteResponse)

        when:
            server.processVoteResponse(voteResponse)

        then: "should update votes map but not convert m_state"
            state.isCandidate() >> true
            voteResponse.isVoteGranted() >> false
            voteResponse.getSenderId() >> 1

            1 * state.updateVote(1, false)
            0 * state.convertStateToLeader()
    }

    def "test granted vote"() {
        setup:
            def voteResponse = Mock(VoteResponse)
            def appendRequest = Mock(AppendEntriesRequest)

            state.isCandidate() >> true
            voteResponse.isVoteGranted() >> true
            context.getServerCount() >> 3
            context.getOtherServerIds() >> (1..2).collect {it as short}
        when:
            server.processVoteResponse(voteResponse)

        then: "should update votes map"
            voteResponse.getSenderId() >> 1
            state.getVotesCount() >> 1

            1 * state.updateVote(1, true)
            0 * state.convertStateToLeader()

        when:
            server.processVoteResponse(voteResponse)

        then: "should update votes map, convert to leader and send heartbeats"
            voteResponse.getSenderId() >> 2
            state.getVotesCount() >> 2

            1 * state.updateVote(2, true)
            1 * state.convertStateToLeader()
            2 * serverMessageFactory.newAppendEntriesRequest(*_) >> appendRequest
            2 * netService.sendMessage(appendRequest)
    }

    def "test success of append entries request handler"() {
        given:
            def request = Mock(AppendEntriesRequest)
            def response = Mock(AppendEntriesResponse)

        when:
            server.processAppendEntriesRequest(request)

        then:
            request.getTerm() >> msgTerm
            state.getCurrentTerm() >> localTerm
            log.isDiffering(_,_) >> diff

            serverMessageFactory.newAppendEntriesResponse(_, localTerm, success, _) >> response
            1 * netService.sendMessage(response)
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
            def request = Mock(DefaultAppendEntriesRequest)

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
            def request = Mock(DefaultAppendEntriesRequest)

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
            def request = Mock(AppendEntriesRequest)
            def logEntry = Mock(LogEntry)
        when:
            server.processAppendEntriesResponse(response)

        then:
            response.isSuccess() >> false
            response.getSenderId() >> 1

            state.isLeader() >> isLeader
            log.getEntryByIndex(_) >> logEntry
            state.getNextIndex(1) >> 1

            doSomething * state.decrementNextIndex(1)
            doSomething * serverMessageFactory.newAppendEntriesRequest(1, _, 0, _, _, _) >> request
            doSomething * netService.sendMessage(request)

        where:
            isLeader || doSomething
            false || 0
            true  || 1
    }

    def "test successful append entries request"() {
        given:
            def response = Mock(DefaultAppendEntriesResponse)
            def requestResponses = (1..2).collect {
                Mock(RequestResponse)
            }

            def logEntries = (1..2).collect {
                def entry = Mock(LogEntry)
                entry
            }

            with (response) {
                isSuccess() >> true
                getSenderId() >> 1
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
            logEntries.eachWithIndex {res, idx ->
                res.buildResponse(responseFactory) >> requestResponses[idx]
            }
            requestResponses.each {
                requestNetService.sendResponse(it)
            }

            //index maps should be updated correctly
            1 * state.updateMatchIndex(1, 3)
            1 * state.updateNextIndex(1, 4)
        }

    def "test redirection"() {
        given:
            def request = Mock(Request)
            def leader = Mock(RaftAddress)
            def response = Mock(RequestResponse)

        when:
            server.processClientRequest(request)

        then:
            state.isLeader() >> false
            state.getCurrentLeader() >> 2
            request.getSenderId() >> 1
            context.getAddressById(2) >> leader

            1 * responseFactory.newRequestResponse(_, _ , leader) >> response
            1 * requestNetService.sendResponse(response)
    }

    /**Read request currently same as write request*/
//    def "test read request"() {
//        given:
//            def request = Mock(ReadRequest)
//            request.isReadRequest() >> true
//
//            def response = Mock(DataResponse)
//            m_state.isLeader() >> true
//
//        when:
//            server.processClientRequest(request)
//
//        then:
//            1 * request.onCommit(*_)
//            1 * request.buildResponse() >> response
//            1 * netService.sendResponse(response)
//    }

    def "test request 1"() {
        given:
            def request = Mock(Request)
            def logEntry = Mock(LogEntry)

            def servers = [1, 2].collect {it as short}
            context.getOtherServerIds() >> servers
            context.singleServerCluster() >> false

            state.isLeader() >> true
            log.contains(_) >> false

        when:
            server.processClientRequest(request)

        then:

            1 * logEntryFactory.getLogEntryFromRequest(request, _) >> logEntry

            // should append to log
            1 * log.append(logEntry)

            // should send append entries request to each follower
            2 * serverMessageFactory.newAppendEntriesRequest(*_)
            2 * netService.sendMessage(*_)
    }

    def "test request 2"() {
        given:
            def request = Mock(Request)
            def logEntry = Mock(LogEntry)
            def servers = [1, 2, 3].collect {it as short}
            context.getOtherServerIds() >> servers
            context.singleServerCluster() >> false

            state.isLeader() >> true
            log.contains(_) >> false

        when:
            server.processClientRequest(request)

        then:
            // should append to log
            1 * logEntryFactory.getLogEntryFromRequest(request, _) >> logEntry
            1 * log.append(logEntry)

            // should send append entries request to each follower
            3 * serverMessageFactory.newAppendEntriesRequest(*_)
            3 * netService.sendMessage(*_)
    }

    def "test duplicate, committed request"() {
        given:
            def request = Mock(Request)
            def logEntry = Mock(LogEntry)
            def response = Mock(RequestResponse)
            logEntry.isCommitted() >> true

            state.isLeader() >> true
            log.contains(_) >> true
            log.getEntryByIndex(_) >> logEntry

        when:
            server.processClientRequest(request)

        then: "should send response because entry is already committed"
            1 * logEntry.updateClientAddress(_)
            1 * logEntry.buildResponse(responseFactory) >> response
            1 * requestNetService.sendResponse(response)
    }

    def "test duplicate, uncommitted request"() {
        given:
            def request = Mock(Request)
            def logEntry = Mock(LogEntry)
            def response = Mock(RequestResponse)
            def address = Mock(RaftAddress)
            logEntry.isCommitted() >> false
            request.getSenderAddress() >> address

            state.isLeader() >> true
            log.contains(_) >> true
            log.getEntryByIndex(_) >> logEntry

        when:
            server.processClientRequest(request)

        then: "should do nothing because entry is not committed"
            1 * logEntry.updateClientAddress(address)
            0 * requestNetService.sendResponse(_)
            0 * netService.sendMessage(_)
    }

    def "test add server request"() {

    }
}
