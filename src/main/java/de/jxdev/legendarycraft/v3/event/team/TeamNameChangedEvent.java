package de.jxdev.legendarycraft.v3.event.team;

import de.jxdev.legendarycraft.v3.event.Event;

public class TeamNameChangedEvent implements Event {
    private final int teamId;
    private final String newName;

    public TeamNameChangedEvent(int teamId, String newName) {
        this.teamId = teamId;
        this.newName = newName;
    }

    public int getTeamId() {
        return teamId;
    }

    public String getNewName() {
        return newName;
    }
}
