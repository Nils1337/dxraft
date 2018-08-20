package de.hhu.bsinfo.dxraft.message.server;


import java.util.List;
import java.util.UUID;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.RaftMessage;

public class ClientResponse extends RaftMessage {

    private boolean success;
    private RaftData value;
    private List<RaftData> listValue;
    private UUID requestId;

    public ClientResponse(RaftAddress receiverAddress, UUID requestId, boolean success) {
        super(receiverAddress);
        this.success = success;
        this.requestId = requestId;
    }

    public ClientResponse(RaftAddress receiverAddress, UUID requestId, RaftData value) {
        super(receiverAddress);
        this.value = value;
        this.requestId = requestId;
    }

    public ClientResponse(RaftAddress receiverAddress, UUID requestId, List<RaftData> value) {
        super(receiverAddress);
        listValue = value;
        this.requestId = requestId;
    }

    public boolean isSuccess() {
        return success;
    }

    public RaftData getValue() {
        return value;
    }

    public List<RaftData> getListValue() {
        return listValue;
    }

    public UUID getRequestId() {
        return requestId;
    }
}
