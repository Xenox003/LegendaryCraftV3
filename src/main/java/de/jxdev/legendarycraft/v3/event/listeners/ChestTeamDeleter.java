package de.jxdev.legendarycraft.v3.event.listeners;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.data.models.team.Team;
import de.jxdev.legendarycraft.v3.event.EventListener;
import de.jxdev.legendarycraft.v3.event.team.TeamDeletedEvent;
import de.jxdev.legendarycraft.v3.service.TeamService;

public class ChestTeamDeleter {

    public EventListener<TeamDeletedEvent> onTeamDelete() {
        return event -> {
            LegendaryCraft.getInstance().getLockedChestCache().deIndexTeam(event.getTeamId());
        };
    }
}
