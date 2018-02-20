package de.hhu.bsinfo.dxraft;

public class RaftServer {

    /* persistent state */
    private RaftState state = RaftState.FOLLOWER;
    private int currentTerm = 0;
    private short votedFor = 0;

    /* volatile state */
    private int commitIndex = 0;
    private int lastApplied = 0;
    private int[] nextIndex;
    private int[] matchIndex;

    public RaftServer() {

    }
}
