package de.jxdev.legendarycraft.v3.events;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.models.Team;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Optional;

public class PlayerJoinListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
           Player player = event.getPlayer();
        Optional<Team> team = LegendaryCraft.getInstance().getTeamService().getPlayerTeam(player.getUniqueId());

        if (team.isPresent()) {
            Component displayName = team.get().getPrefixComponent()
                    .append(Component.text(" " + player.getName()));

            player.displayName(displayName);
            player.playerListName(displayName);

            // TODO: Set players above head prefix
        }

    }
}
