package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.context.RaftID;
import de.hhu.bsinfo.dxraft.server.ServerMessageReceiver;

import java.io.Serializable;

public class RaftMessage implements Serializable {
    protected RaftID receiverId;
    protected RaftAddress senderAddress;
    protected RaftAddress receiverAddress;

    protected RaftMessage(RaftID receiverId) {
        this.receiverId = receiverId;
    }

    protected RaftMessage(RaftAddress receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public RaftAddress getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(RaftAddress senderAddress) {
        this.senderAddress = senderAddress;
    }

    public RaftAddress getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(RaftAddress receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public RaftID getSenderId() {
        return senderAddress.getId();
    }

    public RaftID getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(RaftID receiverId) {
        this.receiverId = receiverId;
    }
}
