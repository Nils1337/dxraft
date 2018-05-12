package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;
import de.hhu.bsinfo.dxraft.data.RaftData;

public class ClientRequest extends RaftMessage implements MessageDeliverer {

    public enum RequestType {
        GET, PUT, DELETE
    }

    private RequestType requestType;
    private String path;
    private RaftData value;

    public ClientRequest(short senderId, short receiverId, RequestType requestType, String path) {
        super(senderId, receiverId);
        this.requestType = requestType;
        this.path = path;
    }

    public ClientRequest(short senderId, short receiverId, RequestType requestType, String path, RaftData value) {
        super(senderId, receiverId);
        this.requestType = requestType;
        this.path = path;
        this.value = value;
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

    public void setSenderId(short senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(short receiverId) {
        this.receiverId = receiverId;
    }
}
