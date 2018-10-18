package de.hhu.bsinfo.dxraft.client.message;

import de.hhu.bsinfo.dxraft.data.RaftAddress;
import de.hhu.bsinfo.dxraft.data.RaftData;

import java.util.List;

public interface RequestFactory {
    Request getReadRequest(String p_dataName);
    Request getReadListRequest(String p_dataName);

    Request getWriteRequest(String p_dataName, RaftData p_data,
                            boolean p_overwrite);
    Request getCompareAndSetRequest(String p_dataName, RaftData p_data,
                                    RaftData p_compareValue);
    Request getWriteListRequest(String p_dataName, List<RaftData> p_data,
                                boolean p_overwrite);
    Request getAppendToListRequest(String p_dataName, RaftData p_data);

    Request getDeleteRequest(String p_dataName);
    Request getDeleteListRequest(String p_dataName);
    Request getDeleteFromListRequest(String p_dataName, RaftData p_data);

    Request getAddServerRequest(RaftAddress p_serverAddress);
    Request getRemoveServerRequest( RaftAddress p_serverAddress);
}
