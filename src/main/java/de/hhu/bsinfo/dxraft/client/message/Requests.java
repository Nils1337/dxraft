package de.hhu.bsinfo.dxraft.client.message;

public final class Requests {

    private Requests() {}

    public static final byte READ_REQUEST = 0;
    public static final byte READ_LIST_REQUEST = 1;
    public static final byte WRITE_REQUEST = 2;
    public static final byte WRITE_LIST_REQUEST = 3;
    public static final byte DELETE_REQUEST = 4;
    public static final byte DELETE_LIST_REQUEST = 5;
    public static final byte CONFIG_CHANGE_REQUEST = 6;
}
