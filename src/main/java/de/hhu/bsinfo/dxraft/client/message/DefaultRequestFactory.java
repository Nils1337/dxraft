package de.hhu.bsinfo.dxraft.client.message;

import de.hhu.bsinfo.dxraft.data.ListData;
import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;
import de.hhu.bsinfo.dxraft.log.entry.ConfigChangeEntry;
import de.hhu.bsinfo.dxraft.log.entry.DeleteListEntry;
import de.hhu.bsinfo.dxraft.log.entry.WriteEntry;
import de.hhu.bsinfo.dxraft.log.entry.WriteListEntry;

import java.util.List;
import java.util.UUID;

public class DefaultRequestFactory implements RequestFactory {

    @Override
    public Request getReadRequest(String p_name) {
        DefaultRequest request = new DefaultRequest();
        request.setRequestType(Requests.READ_REQUEST);
        request.setDataName(p_name);
        request.setId(getNewId());
        return request;
    }

    @Override
    public Request getReadListRequest(String p_dataName) {
        DefaultRequest request = new DefaultRequest();
        request.setRequestType(Requests.READ_LIST_REQUEST);
        request.setDataName(p_dataName);
        request.setId(getNewId());
        return request;
    }

    @Override
    public Request getWriteRequest(String p_dataName, RaftData p_data, boolean p_overwrite) {
        DefaultRequest request = new DefaultRequest();
        request.setRequestType(Requests.WRITE_REQUEST);
        request.setDataName(p_dataName);
        request.setData(p_data);
        request.setRequestMode(p_overwrite ? WriteEntry.OVERWRITE_MODE : WriteEntry.NO_OVERWRITE_MODE);
        request.setId(getNewId());
        return request;
    }

    @Override
    public Request getCompareAndSetRequest(String p_dataName, RaftData p_data,
                                           RaftData p_compareValue) {
        DefaultRequest request = new DefaultRequest();
        request.setRequestType(Requests.WRITE_LIST_REQUEST);
        request.setDataName(p_dataName);
        request.setData(p_data);
        request.setAdditionalData(p_compareValue);
        request.setRequestMode(WriteEntry.COMPARE_AND_SET_MODE);
        request.setId(getNewId());
        return request;
    }

    @Override
    public Request getWriteListRequest(String p_dataName, List<RaftData> p_data,
                                       boolean p_overwrite) {
        DefaultRequest request = new DefaultRequest();
        request.setRequestType(Requests.WRITE_LIST_REQUEST);
        request.setDataName(p_dataName);
        request.setData(new ListData(p_data));
        request.setRequestMode(p_overwrite ? WriteListEntry.OVERWRITE_MODE : WriteListEntry.NO_OVERWRITE_MODE);
        request.setId(getNewId());
        return request;
    }

    @Override
    public Request getAppendToListRequest(String p_dataName, RaftData p_data) {
        DefaultRequest request = new DefaultRequest();
        request.setRequestType(Requests.WRITE_LIST_REQUEST);
        request.setDataName(p_dataName);
        request.setData(p_data);
        request.setRequestMode(WriteListEntry.APPEND_TO_LIST_MODE);
        request.setId(getNewId());
        return request;
    }

    @Override
    public Request getDeleteRequest(String p_dataName) {
        DefaultRequest request = new DefaultRequest();
        request.setRequestType(Requests.DELETE_REQUEST);
        request.setDataName(p_dataName);
        request.setId(getNewId());
        return request;
    }

    @Override
    public Request getDeleteListRequest(String p_dataName) {
        DefaultRequest request = new DefaultRequest();
        request.setRequestType(Requests.DELETE_LIST_REQUEST);
        request.setDataName(p_dataName);
        request.setRequestMode(DeleteListEntry.DELETE_LIST_MODE);
        request.setId(getNewId());
        return request;
    }

    @Override
    public Request getDeleteFromListRequest(String p_dataName, RaftData p_data) {
        DefaultRequest request = new DefaultRequest();
        request.setRequestType(Requests.DELETE_LIST_REQUEST);
        request.setDataName(p_dataName);
        request.setData(p_data);
        request.setRequestMode(DeleteListEntry.DELETE_ITEM_FROM_LIST_MODE);
        request.setId(getNewId());
        return request;
    }

    @Override
    public Request getAddServerRequest(RaftAddress p_serverAddress) {
        DefaultRequest request = new DefaultRequest();
        request.setRequestType(Requests.CONFIG_CHANGE_REQUEST);
        request.setData(p_serverAddress);
        request.setRequestMode(ConfigChangeEntry.ADD_SERVER_MODE);
        request.setId(getNewId());
        return request;
    }

    @Override
    public Request getRemoveServerRequest(RaftAddress p_serverAddress) {
        DefaultRequest request = new DefaultRequest();
        request.setRequestType(Requests.CONFIG_CHANGE_REQUEST);
        request.setData(p_serverAddress);
        request.setRequestMode(ConfigChangeEntry.REMOVE_SERVER_MODE);
        request.setId(getNewId());
        return request;
    }

    private UUID getNewId() {
        return UUID.randomUUID();
    }
}
