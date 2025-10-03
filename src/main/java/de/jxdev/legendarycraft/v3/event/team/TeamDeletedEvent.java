package de.jxdev.legendarycraft.v3.event.team;

import de.jxdev.legendarycraft.v3.event.Event;

public class TeamDeletedEvent implements Event {
    private final int teamId;

    public TeamDeletedEvent(int teamId) {
        this.teamId = teamId;
    }

    public int getTeamId() {
        return teamId;
    }
}
