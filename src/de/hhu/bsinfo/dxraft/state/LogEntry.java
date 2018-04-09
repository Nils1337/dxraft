package de.hhu.bsinfo.dxraft.state;

public class LogEntry {
    public enum LogEntryType {
        PUT, DELETE
    }

    private int term;
    private String path;
    private LogEntryType logEntryType;
    private Object value;

    public LogEntry(int term, String path, LogEntryType logEntryType, Object value) {
        this.term = term;
        this.path = path;
        this.logEntryType = logEntryType;
        this.value = value;
    }

    public void setValue(Object value) {
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

    public Object getValue() {
        return value;
    }
}
