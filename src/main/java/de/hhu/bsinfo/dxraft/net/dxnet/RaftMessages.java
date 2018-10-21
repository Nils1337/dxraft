package de.hhu.bsinfo.dxraft.net.dxnet;

public final class RaftMessages {
    public static final byte DXRAFT_MESSAGE = 1;

    public static final byte APPEND_ENTRIES_REQUEST = 0;
    public static final byte APPEND_ENTRIES_RESPONSE = 1;
    public static final byte VOTE_REQUEST = 2;
    public static final byte VOTE_RESPONSE = 3;

    /**
     * Static class
     */
    private RaftMessages() {
    }
}
