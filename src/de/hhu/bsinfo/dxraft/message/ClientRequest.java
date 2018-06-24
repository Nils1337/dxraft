package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;
import de.hhu.bsinfo.dxraft.data.RaftData;

import java.util.Objects;
import java.util.UUID;

public class ClientRequest extends RaftMessage implements MessageDeliverer {

    public enum RequestType {
        GET, PUT, DELETE
    }

    private RequestType requestType;
    private String path;
    private RaftData value;
    private UUID id;

    public ClientRequest(RaftID receiverId, RequestType requestType, String path) {
        this(receiverId, requestType, path, null);
    }

    public ClientRequest(RaftAddress receiverAddress, RequestType requestType, String path) {
        this(receiverAddress, requestType, path, null);
    }

    public ClientRequest(RaftID receiverId, RequestType requestType, String path, RaftData value) {
        super(receiverId);
        this.requestType = requestType;
        this.path = path;
        this.value = value;
        id = UUID.randomUUID();
    }

    public ClientRequest(RaftAddress receiverAddress, RequestType requestType, String path, RaftData value) {
        super(receiverAddress);
        this.requestType = requestType;
        this.path = path;
        this.value = value;
        id = UUID.randomUUID();
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public boolean isWriteRequest() {
        return requestType == RequestType.PUT;
    }

    public boolean isReadRequest() {
        return requestType == RequestType.GET;
    }

    public boolean isDeleteRequest() {
        return requestType == RequestType.DELETE;
    }

    public String getPath() {
        return path;
    }

    public RaftData getValue() {
        return value;
    }

    @Override
    public void deliverMessage(ServerMessageReceiver messageReceiver) {
        messageReceiver.processClientRequest(this);
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientRequest request = (ClientRequest) o;
        return Objects.equals(id, request.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
