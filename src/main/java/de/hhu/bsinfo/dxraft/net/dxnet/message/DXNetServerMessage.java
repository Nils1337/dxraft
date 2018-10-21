package de.hhu.bsinfo.dxraft.net.dxnet.message;

import de.hhu.bsinfo.dxnet.core.Message;
import de.hhu.bsinfo.dxraft.server.message.ServerMessage;

public abstract class DXNetServerMessage extends Message implements ServerMessage {
    public DXNetServerMessage(short p_destination, byte p_type, byte p_subtype) {
        super(p_destination, p_type, p_subtype);
    }
}
