package de.hhu.bsinfo.dxraft.state

import de.hhu.bsinfo.dxraft.log.Log
import de.hhu.bsinfo.dxraft.log.LogEntry
import de.hhu.bsinfo.dxraft.log.LogStorage
import de.hhu.bsinfo.dxraft.server.ServerContext
import spock.lang.Specification


class LogSpec extends Specification {

    def stateMachine = Mock(StateMachine)
    def context = Mock(ServerContext)
    def logStorage = Mock(LogStorage)
    def log = new Log(context)

    def logEntries = []

    def setup() {
        log.setStateMachine(stateMachine)
        log.setLogStorage(logStorage)

        logStorage.getSize() >> 3

        3.times {
            def entry = Mock(LogEntry)
            entry.getTerm() >> it + 1
            logEntries.push(entry)
        }

        logStorage.getEntriesByRange(_,_) >> {int fromIndex, int toIndex ->
            logEntries[fromIndex..toIndex]
        }

        logStorage.getEntryByIndex(_) >> {int index ->
            logEntries[index]
        }
    }


    def getNewEntries() {
        (1..3).collect({
            def entry = Mock(LogEntry)
            entry.getTerm() >> it + 1
            entry
        })
    }

    def "test update commit index"() {
        when:
        log.commitEntries(1)

        then:
        log.getCommitIndex() == 1
        logEntries.each {entry -> 1 * entry.onCommit(*_)}

        when:
        log.commitEntries(3)

        then:
        thrown(IllegalArgumentException)

        when:
        log.commitEntries(0)

        then:
        thrown(IllegalArgumentException)
    }

    def "test up to date check"() {
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

    def "test differing check"() {
        expect:
            log.isDiffering(prevIndex, prevTerm) == dif
        where:
            prevIndex | prevTerm || dif
            1 | 1 || true
            1 | 3 || true
            1 | 2 || false
            5 | 1 || true
    }

    def "test update log 1"() {
        setup:
            def newEntries = getNewEntries()
            log.commitEntries(0)
        when:
            log.updateEntries(-1, newEntries)
        then:
            thrown(IllegalArgumentException)
    }

    def "test update log 2"() {
        setup:
            def newEntries = getNewEntries()
        when:
            log.updateEntries(0, newEntries)
        then:
            1 * logStorage.removeEntriesByRange(0, 3)
            newEntries.each {entry -> 1 * logStorage.append(entry)}
    }

    def "test update log 3"() {
        setup:
            def newEntries = getNewEntries()
        when:
            log.updateEntries(2, newEntries)
        then:
            1 * logStorage.removeEntriesByRange(2, 3)
            newEntries.each {entry -> 1 * logStorage.append(entry)}
    }


    def "test update log 4"() {
        setup:
        def newEntry = Mock(LogEntry)
        newEntry.getTerm() >> 3

        when:
        log.updateEntries(2, [newEntry])

        then:
        0 * logStorage.removeEntriesByRange(_,_)
        0 * log.append(_)
    }

}