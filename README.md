# DXRaft

DXRaft is a lightweight implementation of the Raft Consensus Algorithm. It is intended to be used with [DXRAM](https://github.com/hhu-bsinfo/dxram) but can be used standalone as well.

## Build

To build DXRaft, run
```
gradle jar
```
The jar can now be found in build/libs

## Run Server

First create a config file (an example can be found in src/main/resources/config.json). 
It should at least contain all servers that will be part of the cluster, e.g.
```JSON
{
    "m_servers": [
        {
            "m_id": 1,
            "m_ip": "127.0.0.1",
            "m_port": 5454
        }
    ]
}
```
if only one local server should be part of the cluster

To run a server instance, do
```
java -Dconfig=config.json -Dserver.id=1 -jar dxraft-0.1.jar
```
with the path to the created config and the server id matching an id in the config file

## Run Client

To test the cluster with a client, run
```
java -Dservers=127.0.0.1:5454 -cp dxraft-0.1.jar de.hhu.bsinfo.dxraft.client.RaftClient
```
with -Dservers being a list of addresses which run raft servers separated by commas. Now you can run commands to test the cluster. For example:

```
>> write test 123
Write successful!
>> read test
123
>> delete test
Deletion of "test" was successful!
```
