package de.hhu.bsinfo.dxraft

import de.hhu.bsinfo.dxraft.client.RaftClient
import de.hhu.bsinfo.dxraft.context.RaftAddress
import de.hhu.bsinfo.dxraft.context.RaftID
import de.hhu.bsinfo.dxraft.data.StringData
import de.hhu.bsinfo.dxraft.server.RaftServer
import de.hhu.bsinfo.dxraft.server.RaftServerContext
import spock.lang.Specification
import spock.lang.Unroll


class LocalTest extends Specification {

    def static serverCount = 3
    def static portFrom = 5000

    def servers = []
    def serverAddresses = []
    def client

    def data = new StringData("data")
    def data2 = new StringData("data2")

    def setup() {

        serverCount.times {
            serverAddresses << new RaftAddress(new RaftID(it), "127.0.0.1", portFrom + it)
        }

        serverCount.times {
            def localAddress = new RaftAddress(new RaftID(it), "127.0.0.1", portFrom + it)

            def context = RaftServerContext.RaftServerContextBuilder
                .aRaftServerContext()
                .withLocalAddress(localAddress)
                .withRaftServers(serverAddresses)
                .build()

            def server = RaftServer.RaftServerBuilder
                .aRaftServer()
                .withContext(context)
                .build()

            servers << server
        }


        servers.each { server ->
            server.bootstrapNewCluster()
        }

        def localAddress = new RaftAddress("127.0.0.1")

        def context = RaftServerContext.RaftServerContextBuilder
            .aRaftServerContext()
            .withLocalAddress(localAddress)
            .withRaftServers(serverAddresses)
            .build()

        client = new RaftClient(context)
    }

    def "test normal requests"() {
        expect:
        client.write("test", data, false)
        client.exists("test")
        client.read("test") == data
        !client.write("test", data2, false)
        client.write("test", data2, true)
        client.read("test") == data2
        client.delete("test") == data2
        !client.exists("test")
        client.read("test") == null
    }

    def "test list requests"() {
        expect:
        client.writeList("test", [], false)
        client.listExists("test")
        client.readList("test") == []
        client.deleteList("test") == []
        !client.listExists("test")

        !client.addToList("test", data, false)
        client.addToList("test", data, true)
        client.readList("test") == [data]
        client.addToList("test", data2, false)
        client.readList("test") == [data, data2]
        client.removeFromList("test", data, false)
        client.readList("test") == [data2]
        client.removeFromList("test", data2, true)
        !client.listExists("test")
        client.readList("test") == null
    }

    @Unroll
    def "test server crash"() {
        expect:

        client.write("test", data, false)

        servers[0].shutdown()

        client.getCurrentLeader() in serverAddresses[1..2]

        client.read("test") == data

        where:
        i << (1..10)

    }

    def cleanup() {
        servers.each {server ->
            server.shutdown()
        }

        client.shutdown()
    }
}