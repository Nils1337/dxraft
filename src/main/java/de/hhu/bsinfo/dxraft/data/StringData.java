package de.hhu.bsinfo.dxraft.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class StringData implements RaftData {
    private String m_data;
}
