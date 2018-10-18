package de.hhu.bsinfo.dxraft

import de.hhu.bsinfo.dxraft.client.RaftClient
import de.hhu.bsinfo.dxraft.data.RaftAddress
import de.hhu.bsinfo.dxraft.client.ClientConfig
import de.hhu.bsinfo.dxraft.data.StringData
import de.hhu.bsinfo.dxraft.server.RaftServer
import de.hhu.bsinfo.dxraft.server.ServerConfig
import spock.lang.Specification
import spock.lang.Unroll


class LocalTest extends Specification {

    def static serverCount = 3
    def static requestPortFrom = 5000
    def static raftPortFrom = 5454

    def servers = []
    def serverAddresses = []
    def client

    def data = new StringData("m_data")
    def data2 = new StringData("data2")
    def data3 = new StringData("data3")

    def setup() {

        serverCount.times {
            serverAddresses << new RaftAddress(it as short, "127.0.0.1", raftPortFrom + it, requestPortFrom + it)
        }

        serverCount.times {

            def context = new ServerConfig()
            context.setLocalId(it as short)
            context.setIp("127.0.0.1")
            context.setRequestPort(requestPortFrom + it)
            context.setRaftPort(raftPortFrom + it)
            context.setServers(serverAddresses.clone())

            def server = RaftServer.RaftServerBuilder
                .aRaftServer()
                .withContext(context)
                .build()

            servers << server
        }


        servers.each { server ->
            server.bootstrapNewCluster()
        }

        def context = new ClientConfig()
        context.setRaftServers(serverAddresses.clone())
        client = new RaftClient(context)

        // let cluster boot and elect leader
        sleep(200)
    }

    def "test leader election"() {
        when:
        sleep(500)
        def leaderCount = 0
        servers.each {
            if (it.getState().isLeader()) {
                leaderCount++
            }
        }

        then:
        leaderCount == 1
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
        client.writeList("test", [data], false)
        client.listExists("test")
        client.deleteList("test")
        !client.listExists("test")

        client.addToList("test", data)
        client.readList("test") == [data]
        client.addToList("test", data2)
        client.readList("test") == [data, data2]
        client.removeFromList("test", data)
        client.readList("test") == [data2]
        client.removeFromList("test", data2)
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

    @Unroll
    def "test config changes"() {
        setup:

        def newAddress = new RaftAddress(3 as short, "127.0.0.1", raftPortFrom + 3, requestPortFrom + 3)

        def context = new ServerConfig()
        context.setLocalId(3 as short)
        context.setIp("127.0.0.1")
        context.setRequestPort(requestPortFrom + 3)
        context.setRaftPort(raftPortFrom + 3)
        context.setServers(serverAddresses.clone())

        def newServer = RaftServer.RaftServerBuilder
            .aRaftServer()
            .withContext(context)
            .build()

        newServer.joinExistingCluster()

        expect:

        client.getCurrentConfig() == serverAddresses
        client.write("test", data, true)

        client.addServer(newAddress)
        client.getCurrentConfig() == serverAddresses + [newAddress]

        client.read("test") == data

        client.removeServer(serverAddresses[1])

        client.getCurrentLeader() in serverAddresses[0,2] + [newAddress]
        client.getCurrentConfig() == serverAddresses[0,2] + [newAddress]
        client.read("test") == data

        cleanup:
        newServer.shutdown()

        where:
        i << (1..10)

    }

    def "test multiple clients"() {
        setup:

        def localAddress = new RaftAddress("127.0.0.1")

        def context = new ClientConfig()
        context.setRaftServers(serverAddresses)

        def client2 = new RaftClient(context)

        expect:

        client.write("test", data, false)
        client.write("test2", data2, false)

        client2.read("test") == data
        client2.read("test2") == data2

        client2.write("test", data3, true)
        client.read("test") == data3
    }

    def cleanup() {
        servers.each {server ->
            server.shutdown()
        }

        client.shutdown()
    }
}