package de.hhu.bsinfo.dxraft.server.net;

import de.hhu.bsinfo.dxraft.server.message.RequestResponse;

public abstract class AbstractRequestNetworkService {

    private RequestReceiver m_requestReceiver;

    public RequestReceiver getRequestReceiver() {
        return m_requestReceiver;
    }

    public abstract void sendResponse(RequestResponse p_message);

    public abstract void startReceiving();

    public abstract void stopReceiving();

    public abstract void close();

    public void setRequestReceiver(RequestReceiver p_requestReceiver) {
        m_requestReceiver = p_requestReceiver;
    }

}
