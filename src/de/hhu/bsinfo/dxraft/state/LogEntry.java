package de.hhu.bsinfo.dxraft.state;

public class LogEntry {

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

    public int getTerm() {
        return term;
    }

    public String getPath() {
        return path;
    }

    public LogEntryType getLogEntryType() {
        return logEntryType;
    }


    public Object getValue() {
        return value;
    }
}
