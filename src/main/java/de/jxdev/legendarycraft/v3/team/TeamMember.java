package de.jxdev.legendarycraft.v3.team;

import java.util.UUID;

/**
 * TeamMember model mapped to the `team_members` table.
 */
public class TeamMember {
    private UUID user;      // player UUID
    private int teamId;     // FK to teams.id
    private PermissionLevel permission;

    public TeamMember() {}

    public TeamMember(UUID user, int teamId, PermissionLevel permission) {
        this.user = user;
        this.teamId = teamId;
        this.permission = permission;
    }

    public UUID getUser() {
        return user;
    }

    public void setUser(UUID user) {
        this.user = user;
    }

    public int getTeamId() {
        return teamId;
    }

    public void setTeamId(int teamId) {
        this.teamId = teamId;
    }

    public PermissionLevel getPermission() {
        return permission;
    }

    public void setPermission(PermissionLevel permission) {
        this.permission = permission;
    }
}
