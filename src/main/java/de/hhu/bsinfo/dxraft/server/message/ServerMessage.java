package de.hhu.bsinfo.dxraft.server.message;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.server.net.ServerMessageReceiver;

public interface ServerMessage extends ServerMessageDeliverer {
    int getTerm();

    short getReceiverId();

    void setReceiverId(short p_receiverId);

    void setReceiverAddress(RaftAddress p_receiverAddress);

    RaftAddress getReceiverAddress();

    void setSenderAddress(RaftAddress p_senderAddress);

    RaftAddress getSenderAddress();

    short getSenderId();

    void setSenderId(short p_id);

    void deliverMessage(ServerMessageReceiver p_messageReceiver);
}
