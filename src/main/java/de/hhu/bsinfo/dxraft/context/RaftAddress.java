package de.hhu.bsinfo.dxraft.context;

import java.io.Serializable;
import java.util.Objects;

import com.google.gson.JsonElement;

public class RaftAddress implements Serializable {
    public static final int INVALID_ID = -1;

    private int id;
    private String ip;
    private int port = -1;

    public RaftAddress(int id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public RaftAddress(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public RaftAddress(String ip) {
        this.ip = ip;
    }

    public RaftAddress() {}

    public int getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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
        return id == address.id &&
            port == address.port &&
            Objects.equals(ip, address.ip);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, ip, port);
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
