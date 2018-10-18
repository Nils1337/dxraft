package de.hhu.bsinfo.dxraft.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.InetSocketAddress;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RaftAddress implements RaftData {
    public static final short INVALID_ID = -1;

    private short m_id;
    private String m_ip;
    private int m_internalPort = -1;
    private int m_requestPort = -1;

    public RaftAddress(String p_ip, int p_requestPort) {
        m_ip = p_ip;
        m_requestPort = p_requestPort;
    }

    public RaftAddress(String ip) {
        m_ip = ip;
    }

    public InetSocketAddress toInetSocketAddress() {
        return new InetSocketAddress(m_ip, m_internalPort);
    }
}
