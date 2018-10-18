package de.hhu.bsinfo.dxraft.client.message;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class DefaultRequest implements Request, Serializable {
    private int m_receiverId;
    private RaftAddress m_senderAddress;
    private RaftAddress m_receiverAddress;
    private byte m_requestType;
    private RaftData m_data;
    private RaftData m_additionalData;
    private String m_dataName;
    private UUID m_id;
    private byte m_requestMode;
}
