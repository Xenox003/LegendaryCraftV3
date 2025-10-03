package de.jxdev.legendarycraft.v3.event.listeners;

import de.jxdev.legendarycraft.v3.event.EventListener;
import de.jxdev.legendarycraft.v3.event.team.*;
import de.jxdev.legendarycraft.v3.util.TeamUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TeamTagUpdater {

    public EventListener<PlayerAddedToTeamEvent> onPlayerAdded() {
        return event -> {
            UUID playerId = event.getPlayerId();
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                TeamUtil.updatePlayerTag(player);
            }
        };
    }

    public EventListener<PlayerRemovedFromTeamEvent> onPlayerRemoved() {
        return event -> {
            UUID playerId = event.getPlayerId();
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                TeamUtil.removePlayerTag(player);
            }
        };
    }

    public EventListener<TeamColorChangedEvent> onTeamColorChanged() {
        return event -> TeamUtil.updateAllPlayerTags(event.getTeam());
    }

    public EventListener<TeamPrefixChangedEvent> onTeamPrefixChanged() {
        return event -> TeamUtil.updateAllPlayerTags(event.getTeam());
    }

    public EventListener<TeamDeletedEvent> onTeamDeleted() {
        return event -> TeamUtil.removeAllMemberTags(event.getTeamId());
    }

    public EventListener<TeamCreatedEvent> onTeamCreated() {
        return event -> TeamUtil.updateAllPlayerTags(event.getTeam());
    }
}
