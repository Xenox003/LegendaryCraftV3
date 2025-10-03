package de.jxdev.legendarycraft.v3.listener;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.data.models.team.Team;
import de.jxdev.legendarycraft.v3.util.TeamUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;
import java.util.UUID;

public class PlayerJoinLeaveListener implements Listener {
    private final LegendaryCraft plugin = LegendaryCraft.getInstance();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        Optional<Team> playerTeam = plugin.getTeamService().getTeamByPlayer(playerId);

        // Load TeamCache \\
        playerTeam.ifPresent(team -> plugin.getTeamCache().indexPlayer(playerId, team.getId()));

        // Set Name Prefix \\
        playerTeam.ifPresent(team -> TeamUtil.updatePlayerTag(event.getPlayer(), team));

        // Set Join Message \\
        event.joinMessage(Component.translatable("common.message.player_join", event.getPlayer().displayName().color(NamedTextColor.WHITE)).color(NamedTextColor.GOLD));

        // Update Discord presence to reflect new player count
        plugin.updateDiscordPresence();
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        // Set Quit Message \\
        event.quitMessage(Component.translatable("common.message.player_leave", event.getPlayer().displayName().color(NamedTextColor.WHITE)).color(NamedTextColor.DARK_RED));

        // Update TeamCache \\
        plugin.getTeamCache().deIndexPlayer(event.getPlayer().getUniqueId());

        // Update PlayerNameService \\
        plugin.getPlayerNameService().cleanup(event.getPlayer());

        // Update Discord presence to reflect new player count
        plugin.updateDiscordPresence();
    }

}
