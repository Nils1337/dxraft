package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

public class ClientRequest extends RaftMessage implements MessageDeliverer {

    public enum RequestType {
        GET, PUT, DELETE
    }

    private RequestType requestType;
    private String path;
    private Object value;

    public ClientRequest(short senderId, short receiverId, RequestType requestType, String path) {
        super(senderId, receiverId);
        this.requestType = requestType;
        this.path = path;
    }

    public ClientRequest(short senderId, short receiverId, RequestType requestType, String path, Object value) {
        super(senderId, receiverId);
        this.requestType = requestType;
        this.path = path;
        this.value = value;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public String getPath() {
        return path;
    }

    public Object getValue() {
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
