package de.jxdev.legendarycraft.v3.event.team;

import de.jxdev.legendarycraft.v3.data.models.team.Team;
import de.jxdev.legendarycraft.v3.event.Event;

public class TeamColorChangedEvent implements Event {
    private final Team team;

    public TeamColorChangedEvent(Team team) {
        this.team = team;
    }

    public Team getTeam() {
        return team;
    }
}
