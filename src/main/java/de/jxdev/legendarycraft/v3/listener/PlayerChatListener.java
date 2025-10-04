package de.jxdev.legendarycraft.v3.listener;

import de.jxdev.legendarycraft.v3.data.models.team.TeamCacheRecord;
import de.jxdev.legendarycraft.v3.service.TeamChatService;
import de.jxdev.legendarycraft.v3.service.TeamService;
import de.jxdev.legendarycraft.v3.util.ChatUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;
import java.util.Optional;

public class PlayerChatListener implements Listener {
    private final TeamChatService teamChatService;
    private final TeamService teamService;

    public PlayerChatListener(TeamChatService teamChatService, TeamService teamService) {
        this.teamChatService = teamChatService;
        this.teamService = teamService;
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        Player source = event.getPlayer();

        // One-off team chat using leading '!'
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        boolean forceTeam = plain.startsWith("!");

        if (forceTeam || teamChatService.isEnabled(source.getUniqueId())) {
            final Optional<TeamCacheRecord> teamOpt = teamService.getCachedTeamByPlayer(source.getUniqueId());
            teamOpt.ifPresent(team -> sendTeamChat(team, source, plain.substring(forceTeam ? 1 : 0)));
            event.setCancelled(true);
        }

        // Default global chat render
        event.renderer((src, sourceDisplayName, message, viewer) -> Component.empty()
                .append(sourceDisplayName.color(NamedTextColor.WHITE))
                .append(Component.text(" » "))
                .append(message.color(NamedTextColor.WHITE))
                .color(NamedTextColor.GRAY));
    }

    private void sendTeamChat(TeamCacheRecord targetTeam, Player player, String message) {
        Component chatComponent = ChatUtil.getTeamChatComponent(player, Component.text(message));
        for (Player p : Bukkit.getOnlinePlayers()) {
            teamService.getCachedTeamByPlayer(p.getUniqueId()).ifPresent(team -> {
                if (Objects.equals(team.getId(), targetTeam.getId())) {
                    p.sendMessage(chatComponent);
                }
            });
        }
        Bukkit.getConsoleSender().sendMessage(chatComponent);
    }
}
