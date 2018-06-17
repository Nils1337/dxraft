package de.hhu.bsinfo.dxraft.server

import de.hhu.bsinfo.dxraft.state.Log
import de.hhu.bsinfo.dxraft.state.LogEntry
import spock.lang.Specification


class LogSpec extends Specification {

    def log = new Log()
    def logEntries = []

    def setupLog() {
        3.times {
            def entry = Mock(LogEntry)
            logEntries.push(entry)
            log.append(entry)
        }
    }

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
            3.times {
                def entry = new LogEntry(it+1)
                log.append(entry)
            }
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

}