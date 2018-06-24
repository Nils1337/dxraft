package de.hhu.bsinfo.dxraft.context;

import java.io.Serializable;
import java.util.Objects;

public class RaftAddress implements Serializable {
    private RaftID id;
    private String ip;
    private int port;

    public RaftAddress(RaftID id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public RaftAddress(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public RaftID getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RaftAddress that = (RaftAddress) o;
        return port == that.port &&
                Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {

        return Objects.hash(ip, port);
    }

    @Override
    public String toString() {
        return "RaftAddress{" +
                "id=" + id +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}
