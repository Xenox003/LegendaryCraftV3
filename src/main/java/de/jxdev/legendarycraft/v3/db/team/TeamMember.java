package de.jxdev.legendarycraft.v3.db.team;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * TeamMember model mapped to the `team_members` table.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember {
    private UUID user;      // player UUID
    private int teamId;     // FK to teams.id
    private PermissionLevel permission;
}
