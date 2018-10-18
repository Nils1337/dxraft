package de.hhu.bsinfo.dxraft.log;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.log.entry.*;
import de.hhu.bsinfo.dxraft.client.message.Request;
import de.hhu.bsinfo.dxraft.client.message.Requests;

public class LogEntryFactory {
    public LogEntry getLogEntryFromRequest(Request p_request, int p_term) {
        LogEntry logEntry;

        switch (p_request.getRequestType()) {
            case Requests.READ_REQUEST:
                logEntry = new ReadEntry(p_request.getId(), p_request.getSenderAddress(), p_term,
                    p_request.getDataName());
                break;
            case Requests.READ_LIST_REQUEST:
                logEntry = new ReadListEntry(p_request.getId(), p_request.getSenderAddress(), p_term,
                    p_request.getDataName());
                break;
            case Requests.WRITE_REQUEST:
                logEntry = new WriteEntry(p_request.getId(), p_request.getSenderAddress(), p_term,
                    p_request.getDataName(), p_request.getData(),
                    p_request.getRequestMode(), p_request.getAdditionalData());
                break;
            case Requests.WRITE_LIST_REQUEST:
                logEntry = new WriteListEntry(p_request.getId(), p_request.getSenderAddress(), p_term,
                    p_request.getDataName(), p_request.getData(), p_request.getRequestMode());
                break;
            case Requests.DELETE_REQUEST:
                logEntry = new DeleteEntry(p_request.getId(), p_request.getSenderAddress(), p_term,
                    p_request.getDataName());
                break;
            case Requests.DELETE_LIST_REQUEST:
                logEntry = new DeleteListEntry(p_request.getId(), p_request.getSenderAddress(), p_term,
                    p_request.getDataName(), p_request.getData(), p_request.getRequestMode());
                break;
            case Requests.CONFIG_CHANGE_REQUEST:
                logEntry = new ConfigChangeEntry(p_request.getId(), p_request.getSenderAddress(), p_term,
                    (RaftAddress) p_request.getData(), p_request.getRequestMode());
                break;
            default:
                throw new RuntimeException("Unknown Request Type");
        }

        return logEntry;
    }
}
