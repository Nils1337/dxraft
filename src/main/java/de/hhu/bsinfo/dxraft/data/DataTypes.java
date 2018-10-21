package de.hhu.bsinfo.dxraft.data;

public final class DataTypes {
    public static final byte ADDRESS_DATA = 0;
    public static final byte SHORT_DATA = 1;
    public static final byte STRING_DATA = 2;
    public static final byte LIST_DATA = 3;

    private DataTypes() {}

    public static RaftData fromType(byte p_type) {
        RaftData data;

        switch (p_type) {
            case DataTypes.ADDRESS_DATA:
                data = new RaftAddress();
                break;
            case DataTypes.SHORT_DATA:
                data = new ShortData();
                break;
            case DataTypes.STRING_DATA:
                data = new StringData();
                break;
            case DataTypes.LIST_DATA:
                data = new ListData();
                break;
            default:
                throw new RuntimeException("Unknown data type encountered while deserializing list data");
        }

        return  data;
    }
}
