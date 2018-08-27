package de.hhu.bsinfo.dxraft.context;

import java.io.Serializable;
import java.util.Objects;

public class RaftAddress implements Serializable {
    public static final int INVALID_ID = -1;

    private int m_id;
    private String m_ip;
    private int m_port = -1;

    public RaftAddress(int p_id, String p_ip, int p_port) {
        m_id = p_id;
        m_ip = p_ip;
        m_port = p_port;
    }

    public RaftAddress(String p_ip, int p_port) {
        m_ip = p_ip;
        m_port = p_port;
    }

    public RaftAddress(String ip) {
        m_ip = ip;
    }

    public RaftAddress() {}

    public int getId() {
        return m_id;
    }

    public String getIp() {
        return m_ip;
    }

    public int getPort() {
        return m_port;
    }

    public void setPort(int p_port) {
        m_port = p_port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RaftAddress address = (RaftAddress) o;
        return m_id == address.m_id &&
            m_port == address.m_port &&
            Objects.equals(m_ip, address.m_ip);
    }

    @Override
    public int hashCode() {

        return Objects.hash(m_id, m_ip, m_port);
    }

    @Override
    public String toString() {
        return "RaftAddress{" +
                "id=" + m_id +
                ", ip='" + m_ip + '\'' +
                ", port=" + m_port +
                '}';
    }
}
