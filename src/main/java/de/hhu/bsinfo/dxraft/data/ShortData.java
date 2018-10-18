package de.hhu.bsinfo.dxraft.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class ShortData implements RaftData {
    private short m_value;
}
