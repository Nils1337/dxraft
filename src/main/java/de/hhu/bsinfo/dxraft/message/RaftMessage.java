package de.hhu.bsinfo.dxraft.message;

import de.hhu.bsinfo.dxraft.net.RaftAddress;

import java.io.Serializable;

public class RaftMessage implements Serializable {
    private int m_receiverId;
    private RaftAddress m_senderAddress;
    private RaftAddress m_receiverAddress;

    protected RaftMessage() {}

    protected RaftMessage(int p_receiverId) {
        m_receiverId = p_receiverId;
    }

    protected RaftMessage(RaftAddress p_receiverAddress) {
        m_receiverAddress = p_receiverAddress;
    }

    public RaftAddress getSenderAddress() {
        return m_senderAddress;
    }

    public void setSenderAddress(RaftAddress p_senderAddress) {
        m_senderAddress = p_senderAddress;
    }

    public RaftAddress getReceiverAddress() {
        return m_receiverAddress;
    }

    public void setReceiverAddress(RaftAddress p_receiverAddress) {
        m_receiverAddress = p_receiverAddress;
    }

    public int getSenderId() {
        return m_senderAddress.getId();
    }

    public int getReceiverId() {
        return m_receiverId;
    }

    public void setReceiverId(int p_receiverId) {
        m_receiverId = p_receiverId;
    }
}
