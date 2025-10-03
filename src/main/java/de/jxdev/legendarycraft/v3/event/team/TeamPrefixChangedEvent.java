package de.jxdev.legendarycraft.v3.event.team;

import de.jxdev.legendarycraft.v3.data.models.team.Team;
import de.jxdev.legendarycraft.v3.event.Event;

public class TeamPrefixChangedEvent implements Event {
    private final Team team;

    public TeamPrefixChangedEvent(Team team) {
        this.team = team;
    }

    public Team getTeam() {
        return team;
    }
}
