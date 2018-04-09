package de.hhu.bsinfo.dxraft.message;


public class ClientResponse extends RaftClientMessage {

    private boolean success;
    private Object value;

    public ClientResponse(short senderId, short receiverId, boolean success) {
        super(senderId, receiverId);
        this.success = success;
    }

    public ClientResponse(short senderId, short receiverId, Object value) {
        super(senderId, receiverId);
        this.value = value;
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getValue() {
        return value;
    }
}
