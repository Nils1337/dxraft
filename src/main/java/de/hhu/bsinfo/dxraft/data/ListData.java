package de.hhu.bsinfo.dxraft.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ListData implements RaftData {
    private List<RaftData> m_data;
}
