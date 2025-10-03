package de.jxdev.legendarycraft.v3.event.team;

import de.jxdev.legendarycraft.v3.data.models.team.Team;
import de.jxdev.legendarycraft.v3.event.Event;

import java.util.UUID;

public class PlayerRemovedFromTeamEvent implements Event {
    private final Team team;
    private final UUID playerId;

    public PlayerRemovedFromTeamEvent(Team team, UUID playerId) {
        this.team = team;
        this.playerId = playerId;
    }

    public Team getTeam() {
        return team;
    }

    public UUID getPlayerId() {
        return playerId;
    }
}
