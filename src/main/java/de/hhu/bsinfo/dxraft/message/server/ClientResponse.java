package de.hhu.bsinfo.dxraft.message.server;


import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.RaftMessage;

public class ClientResponse extends RaftMessage {

    private boolean success;
    private RaftData value;

    public ClientResponse(RaftAddress receiverAddress, boolean success) {
        super(receiverAddress);
        this.success = success;
    }

    public ClientResponse(RaftAddress receiverAddress, RaftData value) {
        super(receiverAddress);
        this.value = value;
    }

    public boolean isSuccess() {
        return success;
    }

    public RaftData getValue() {
        return value;
    }
}
