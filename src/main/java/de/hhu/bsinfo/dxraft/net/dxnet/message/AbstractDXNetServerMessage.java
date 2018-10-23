package de.hhu.bsinfo.dxraft.net.dxnet.message;

import de.hhu.bsinfo.dxnet.core.Message;
import de.hhu.bsinfo.dxraft.server.message.ServerMessage;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public abstract class AbstractDXNetServerMessage extends Message implements ServerMessage {
    public AbstractDXNetServerMessage(short p_destination, byte p_type, byte p_subtype) {
        super(p_destination, p_type, p_subtype);
    }
}
