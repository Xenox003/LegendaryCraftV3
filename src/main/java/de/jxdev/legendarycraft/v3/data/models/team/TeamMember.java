package de.jxdev.legendarycraft.v3.data.models.team;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember {
    private int teamId;
    private UUID playerId;
    private TeamMemberRole role;
}
