package de.jxdev.legendarycraft.v3.data.models.team;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TeamWithMemberCount extends Team {
    private int memberCount;
}
