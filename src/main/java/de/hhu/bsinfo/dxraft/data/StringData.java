package de.hhu.bsinfo.dxraft.data;

import java.util.Objects;

public class StringData implements RaftData {

    private String m_data;

    public String getData() {
        return m_data;
    }

    public StringData(String p_data) {
        m_data = p_data;
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
        return Objects.equals(m_data, that.m_data);
    }

    @Override
    public int hashCode() {

        return Objects.hash(m_data);
    }

    @Override
    public String toString() {
        return m_data;
    }
}
