package de.hhu.bsinfo.dxraft.data;

import java.util.Objects;

public class ShortData implements RaftData {
    private short m_value;

    public short getData() {
        return m_value;
    }

    public ShortData(short p_value) {
        this.m_value = p_value;
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
        return m_value == shortData.m_value;
    }

    @Override
    public int hashCode() {

        return Objects.hash(m_value);
    }
}
