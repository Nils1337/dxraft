package de.hhu.bsinfo.dxraft.message;


import de.hhu.bsinfo.dxraft.data.RaftData;

public class ClientResponse extends RaftClientMessage {

    private boolean success;
    private RaftData value;

    public ClientResponse(short senderId, short receiverId, boolean success) {
        super(senderId, receiverId);
        this.success = success;
    }

    public ClientResponse(short senderId, short receiverId, RaftData value) {
        super(senderId, receiverId);
        this.value = value;
    }

    public boolean isSuccess() {
        return success;
    }

    public RaftData getValue() {
        return value;
    }
}
