package de.hhu.bsinfo.dxraft.state

import de.hhu.bsinfo.dxraft.context.RaftID
import de.hhu.bsinfo.dxraft.state.Log
import de.hhu.bsinfo.dxraft.state.LogEntry
import de.hhu.bsinfo.dxraft.state.StateMachine
import spock.lang.Shared
import spock.lang.Specification


class LogSpec extends Specification {

    def stateMachine = Mock(StateMachine)
    def log = Log.LogBuilder.aLog()
            .withStateMachine(stateMachine)
            .build()
    def logEntries = []

    def setupLog() {
        3.times {
            def entry = Mock(LogEntry)
            entry.getTerm() >> it + 1
            logEntries.push(entry)
            log.append(entry)
        }
    }

    @Shared
    def id1 = new RaftID(1)
    @Shared
    def id2 = new RaftID(2)
    @Shared
    def id3 = new RaftID(3)

    def "test log state"() {
        setup:
            setupLog()
        expect:
            log.getLastIndex() == 2
            log.getLastEntry().is(logEntries[2])
            log.getSize() == 3
            log.getCommitIndex() == -1
            log.contains(logEntries[1])
            log.getNewestEntries(1) == logEntries.drop(1)
    }

    def "test update commit index"() {
        setup:
            setupLog()
        when:
            log.updateCommitIndex(1)
        then:
            log.getCommitIndex() == 1
        when:
            log.updateCommitIndex(3)
        then:
            thrown(IllegalArgumentException)
        when:
            log.updateCommitIndex(0)
        then:
            thrown(IllegalArgumentException)
    }

    def "test up to date check with empty log"() {
        expect:
            log.isAtLeastAsUpToDateAs(lastTerm, lastIndex)
        where:
            lastTerm | lastIndex
            2 | 1
            1 | 2
            3 | 3
    }

    def "test up to date check"() {
        setup:
            setupLog()
        expect:
            log.isAtLeastAsUpToDateAs(lastTerm, lastIndex) == upd
        where:
            lastTerm | lastIndex || upd
            1 | 1 || false
            3 | 1 || false
            3 | 4 || true
            5 | 1 || true
            4 | 4 || true
    }

    def "test differing check with empty log"() {
        expect:
            log.isDiffering(prevIndex, prevTerm) == dif
        where:
            prevIndex | prevTerm || dif
            1 | 1 || true
            0 | 3 || true
            -1 | -1 || false
    }

    def "test differing check"() {
        setup:
            setupLog()
        expect:
            log.isDiffering(prevIndex, prevTerm) == dif
        where:
            prevIndex | prevTerm || dif
            1 | 1 || true
            1 | 3 || true
            1 | 2 || false
            5 | 1 || true
    }

    def setupUpdateLogTest() {
        setupLog()
        log.updateCommitIndex(0)
        def newEntries = (1..3).collect({
            def entry = Mock(LogEntry)
            entry.getTerm() >> it + 2
            entry
        })
        return newEntries
    }

    def "test update log 1"() {
        setup:
            def newEntries = setupUpdateLogTest()
        when:
            log.updateLog(-1, newEntries)
        then:
            thrown(IllegalArgumentException)
    }

    def "test update log 2"() {
        setup:
            def newEntries = setupUpdateLogTest()
        when:
            def removedEntries = log.updateLog(0, newEntries)
        then:
            log.getSize() == 4
            log.get(0).is(logEntries[0])
            3.times {
                log.get(it+1).is(newEntries[it])
            }

            removedEntries == logEntries[1..-1]
    }

    def "test update log 3"() {
        setup:
            def newEntries = setupUpdateLogTest()
        when:
            def removedEntries = log.updateLog(2, newEntries)
        then:
            log.getSize() == 6
            3.times {
                log.get(it).is(logEntries[it])
            }
            3.times {
                log.get(it+3).is(newEntries[it])
            }

            removedEntries == []
    }

}