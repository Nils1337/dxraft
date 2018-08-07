package de.hhu.bsinfo.dxraft.data;

import java.util.Objects;

public class StringData implements RaftData {

    private String data;

    public String getData() {
        return data;
    }

    public StringData(String data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StringData that = (StringData) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {

        return Objects.hash(data);
    }

    @Override
    public String toString() {
        return data;
    }
}
