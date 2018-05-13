package de.hhu.bsinfo.dxraft.context;

import java.util.Objects;

public class RaftID {
    private short id;

    public RaftID (short id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RaftID raftID = (RaftID) o;
        return id == raftID.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "" + id;
    }
}
