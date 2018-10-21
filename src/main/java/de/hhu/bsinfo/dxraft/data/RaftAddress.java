package de.hhu.bsinfo.dxraft.data;

import de.hhu.bsinfo.dxutils.serialization.Exporter;
import de.hhu.bsinfo.dxutils.serialization.Importer;
import de.hhu.bsinfo.dxutils.serialization.ObjectSize;
import de.hhu.bsinfo.dxutils.serialization.ObjectSizeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.InetSocketAddress;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RaftAddress implements RaftData {
    public static final short INVALID_ID = -1;

    private short m_id;
    private String m_ip;
    private int m_internalPort = -1;
    private int m_requestPort = -1;

    public RaftAddress(String p_ip, int p_requestPort) {
        m_ip = p_ip;
        m_requestPort = p_requestPort;
    }

    public RaftAddress(String ip) {
        m_ip = ip;
    }

    public InetSocketAddress toInetSocketAddress() {
        return new InetSocketAddress(m_ip, m_internalPort);
    }

    @Override
    public void exportObject(Exporter p_exporter) {
        p_exporter.writeByte(DataTypes.ADDRESS_DATA);
        p_exporter.writeShort(m_id);
        p_exporter.writeString(m_ip);
        p_exporter.writeInt(m_internalPort);
        p_exporter.writeInt(m_requestPort);
    }

    @Override
    public void importObject(Importer p_importer) {
        m_id = p_importer.readShort(m_id);
        m_ip = p_importer.readString(m_ip);
        m_internalPort = p_importer.readInt(m_internalPort);
        m_requestPort = p_importer.readInt(m_requestPort);
    }

    @Override
    public int sizeofObject() {
        return ObjectSizeUtil.sizeofString(m_ip) + Byte.BYTES + Short.BYTES + 2 * Integer.BYTES;
    }
}
