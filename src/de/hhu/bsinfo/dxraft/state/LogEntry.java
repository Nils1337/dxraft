package de.hhu.bsinfo.dxraft.state;

import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.message.ClientRequest;
import de.hhu.bsinfo.dxraft.message.ClientResponse;

import java.util.Objects;

public class LogEntry {
    public enum LogEntryType {
        PUT, DELETE
    }

    private int term;
    private ClientRequest clientRequest;
    private ClientResponse clientResponse;
    private LogEntryType logEntryType;

    public LogEntry(int term, ClientRequest clientRequest) {
        this.term = term;
        this.clientRequest = clientRequest;
        switch (clientRequest.getRequestType()) {
            case PUT:
                logEntryType = LogEntryType.PUT;
                break;
            case DELETE:
                logEntryType = LogEntryType.DELETE;
                break;
            default:
                throw new IllegalArgumentException("Request must be a write request!");
        }
    }

//    public void setValue(RaftData value) {
//        this.value = value;
//    }

    public int getTerm() {
        return term;
    }

    public String getPath() {
        return clientRequest.getPath();
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
        return clientRequest.getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return Objects.equals(clientRequest, logEntry.clientRequest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientRequest);
    }

    public ClientResponse getClientResponse() {
        return clientResponse;
    }

    public void setClientResponse(ClientResponse clientResponse) {
        this.clientResponse = clientResponse;
    }

    public void setClientRequest(ClientRequest clientRequest) {
        this.clientRequest = clientRequest;
    }

    public ClientRequest getClientRequest() {
        return clientRequest;
    }
}
