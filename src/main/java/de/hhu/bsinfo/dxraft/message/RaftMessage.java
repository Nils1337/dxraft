package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.context.RaftAddress;

import java.io.Serializable;

public class RaftMessage implements Serializable {
    protected int receiverId;
    protected RaftAddress senderAddress;
    protected RaftAddress receiverAddress;

    protected RaftMessage() {}

    protected RaftMessage(int receiverId) {
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

    public int getSenderId() {
        return senderAddress.getId();
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }
}
