package de.hhu.bsinfo.dxraft.message.server;

import de.hhu.bsinfo.dxraft.message.MessageDeliverer;
import de.hhu.bsinfo.dxraft.message.RaftMessage;

public abstract class AbstractServerMessage extends RaftMessage implements MessageDeliverer {
    private int m_term;

    AbstractServerMessage(int p_receiverId, int p_term) {
        super(p_receiverId);
        m_term = p_term;
    }

    public int getTerm() {
        return m_term;
    }

}
