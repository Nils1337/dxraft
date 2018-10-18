package de.hhu.bsinfo.dxraft.client.message;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;

import java.util.List;
import java.util.UUID;

public interface Request {
    RaftAddress getReceiverAddress();

    void setReceiverAddress(RaftAddress p_address);

    RaftAddress getSenderAddress();

    void setSenderAddress(RaftAddress p_address);

    byte getRequestType();

    RaftData getData();

    RaftData getAdditionalData();

    String getDataName();

    UUID getId();

    byte getRequestMode();
}
