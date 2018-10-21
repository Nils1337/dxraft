package de.hhu.bsinfo.dxraft.data;

import de.hhu.bsinfo.dxutils.serialization.Exporter;
import de.hhu.bsinfo.dxutils.serialization.Importer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShortData implements RaftData {
    private short m_value;

    @Override
    public void exportObject(Exporter p_exporter) {
        p_exporter.writeByte(DataTypes.SHORT_DATA);
        p_exporter.writeShort(m_value);
    }

    @Override
    public void importObject(Importer p_importer) {
        m_value = p_importer.readShort(m_value);
    }

    @Override
    public int sizeofObject() {
        return Byte.BYTES + Short.BYTES;
    }
}
