package de.hhu.bsinfo.dxraft.data;

import java.util.Objects;

public class ShortData implements RaftData {
    private short value;

    public short getData() {
        return value;
    }

    public ShortData(short value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ShortData shortData = (ShortData) o;
        return value == shortData.value;
    }

    @Override
    public int hashCode() {

        return Objects.hash(value);
    }
}
