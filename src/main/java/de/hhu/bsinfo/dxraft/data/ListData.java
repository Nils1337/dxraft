package de.hhu.bsinfo.dxraft.data;

import de.hhu.bsinfo.dxutils.serialization.Exporter;
import de.hhu.bsinfo.dxutils.serialization.Importer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListData implements RaftData {
    private List<RaftData> m_data;

    @Override
    public void exportObject(Exporter p_exporter) {
        p_exporter.writeByte(DataTypes.LIST_DATA);
        p_exporter.writeInt(m_data.size());
        for (RaftData data: m_data) {
            p_exporter.exportObject(data);
        }
    }

    @Override
    public void importObject(Importer p_importer) {
        int size = 0;
        size = p_importer.readInt(size);

        m_data = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            byte dataType = 0;
            dataType = p_importer.readByte(dataType);

            RaftData data = DataTypes.fromType(dataType);
            p_importer.importObject(data);
            m_data.add(data);
        }

    }

    @Override
    public int sizeofObject() {
        int size = 0;
        for (RaftData data: m_data) {
            size += data.sizeofObject();
        }
        return size + Byte.BYTES + Integer.BYTES;
    }
}
