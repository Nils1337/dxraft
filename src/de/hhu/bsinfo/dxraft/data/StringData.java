package de.hhu.bsinfo.dxraft.data;

public class StringData implements RaftData {
    private String data;

    public StringData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return data;
    }
}
