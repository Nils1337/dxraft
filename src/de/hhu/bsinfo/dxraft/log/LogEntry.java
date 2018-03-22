package de.hhu.bsinfo.dxraft.log;

public class LogEntry {

    private int term;
    private String path;
    private LogEntryType logEntryType;
    private byte[] value;

    public int getTerm() {
        return term;
    }

    public String getPath() {
        return path;
    }

    public LogEntryType getLogEntryType() {
        return logEntryType;
    }

    public byte[] getValue() {
        return value;
    }
}
