package de.hhu.bsinfo.dxraft.data;

import de.hhu.bsinfo.dxutils.serialization.Exporter;
import de.hhu.bsinfo.dxutils.serialization.Importer;
import de.hhu.bsinfo.dxutils.serialization.ObjectSizeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StringData implements RaftData {
    private String m_data;

    @Override
    public void exportObject(Exporter p_exporter) {
        p_exporter.writeByte(DataTypes.STRING_DATA);
        p_exporter.writeString(m_data);
    }

    @Override
    public void importObject(Importer p_importer) {
        m_data = p_importer.readString(m_data);
    }

    @Override
    public int sizeofObject() {
        return ObjectSizeUtil.sizeofString(m_data) + Byte.BYTES;
    }
}
