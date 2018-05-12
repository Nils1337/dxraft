package de.hhu.bsinfo.dxraft.state;

import de.hhu.bsinfo.dxraft.data.RaftData;

public class LogEntry {
    public enum LogEntryType {
        PUT, DELETE
    }

    private int term;
    private String path;
    private LogEntryType logEntryType;
    private RaftData value;

    public LogEntry(int term, String path, LogEntryType logEntryType, RaftData value) {
        this.term = term;
        this.path = path;
        this.logEntryType = logEntryType;
        this.value = value;
    }

    public void setValue(RaftData value) {
        this.value = value;
    }

    public int getTerm() {
        return term;
    }

    public String getPath() {
        return path;
    }

    public LogEntryType getLogEntryType() {
        return logEntryType;
    }

    public boolean isWriting() {
        return logEntryType == LogEntryType.PUT;
    }

    public boolean isDeletion() {
        return logEntryType == LogEntryType.DELETE;
    }

    public RaftData getValue() {
        return value;
    }
}
