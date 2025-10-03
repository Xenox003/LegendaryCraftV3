package de.jxdev.legendarycraft.v3.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.data.models.team.Team;
import de.jxdev.legendarycraft.v3.data.models.team.TeamCacheRecord;
import de.jxdev.legendarycraft.v3.exception.team.TeamServiceException;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.Optional;

public class TeamUtil {
    private static final LegendaryCraft plugin = LegendaryCraft.getInstance();

    private static final SimpleCommandExceptionType PLAYER_NOT_IN_TEAM = new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(
            Component.translatable("team.error.member_required", NamedTextColor.RED)
    ));
    private static final DynamicCommandExceptionType PLAYER_DOESNT_OWN_TEAM = new DynamicCommandExceptionType(team -> MessageComponentSerializer.message().serialize(
            Component.translatable("team.error.no_owner", ((TeamCacheRecord)team).getChatComponent())
                    .color(NamedTextColor.RED)
    ));
    private static final DynamicCommandExceptionType PLAYER_ALREADY_IN_TEAM = new DynamicCommandExceptionType(team -> MessageComponentSerializer.message().serialize(
            Component.translatable("team.error.no_member_required")
                    .append(Component.text("\n"))
                    .append(Component.translatable("team.info.current_team", ((TeamCacheRecord)team).getChatComponent()))
                    .color(NamedTextColor.RED)
    ));

    public static Component getChatComponent(String name, NamedTextColor color) {
        Style style = Style.style(color, TextDecoration.UNDERLINED);
        return Component.text(name, style)
                .clickEvent(ClickEvent.runCommand("/team info " + name))
                .hoverEvent(Component.translatable("team.info.show_more"));
    }

    /**
     * Gets the current player's team
     * @return the current player's team
     * @throws CommandSyntaxException when the player is not in a team
     */
    public static Team getCurrentPlayerTeam(Player player) throws CommandSyntaxException {
        Optional<Team> team = plugin.getTeamService().getTeamByPlayer(player.getUniqueId());
        if (team.isEmpty()) {
            throw PLAYER_NOT_IN_TEAM.create();
        }

        return team.get();
    }

    /**
     * Get the current player's team from cache
     * @return the current player's TeamCacheRecord
     * @throws CommandSyntaxException when the player is not in a team
     */
    public static TeamCacheRecord getCurrentPlayerTeamFromCache(Player player) throws CommandSyntaxException {
        Optional<TeamCacheRecord> team = plugin.getTeamService().getCachedTeamByPlayer(player.getUniqueId());
        if (team.isEmpty()) {
            throw PLAYER_NOT_IN_TEAM.create();
        }

        return team.get();
    }

    /**
     * Checks if the given player has no team.
     * @throws CommandSyntaxException when the player already has a team
     */
    public static void checkIfPlayerHasNoTeam(Player player) throws CommandSyntaxException {
        Optional<TeamCacheRecord> team = plugin.getTeamService().getCachedTeamByPlayer(player.getUniqueId());
        if (team.isPresent()) {
            throw PLAYER_ALREADY_IN_TEAM.create(team.get());
        }
    }

    /**
     * Checks if the given player owns the given team.
     * @throws CommandSyntaxException when the player doesn't own the team
     */
    public static void checkPlayerOwnsTeam(Player player, TeamCacheRecord team) throws CommandSyntaxException {
        if (!plugin.getTeamService().isPlayerTeamOwner(player.getUniqueId(), team)) {
            throw PLAYER_DOESNT_OWN_TEAM.create(team);
        }
    }

    public static void updatePlayerTag(Player player) {
        Optional<Team> team = plugin.getTeamService().getTeamByPlayer(player.getUniqueId());
        if (team.isEmpty()) {
            removePlayerTag(player);
        } else {
            updatePlayerTag(player, team.get());
        }
    }
    public static void removePlayerTag(Player player) {
        plugin.getPlayerNameService().clearPrefix(player);
    }
    public static void updatePlayerTag(Player player, Team team) {
        plugin.getPlayerNameService().setPrefix(player, team.getPrefixComponent());
    }
    public static void updateAllPlayerTags(Team team) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Optional<TeamCacheRecord> playerTeam = plugin.getTeamService().getCachedTeamByPlayer(player.getUniqueId());
            if (playerTeam.isEmpty()) {
                removePlayerTag(player);
                continue;
            };
            if (playerTeam.get().getId() != team.getId()) continue;
            updatePlayerTag(player, team);
        }
    }
    public static void removeAllMemberTags(int teamId) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Optional<TeamCacheRecord> playerTeam = plugin.getTeamService().getCachedTeamByPlayer(player.getUniqueId());
            if (playerTeam.isEmpty()) continue;
            if (playerTeam.get().getId() != teamId) continue;
            removePlayerTag(player);
        }
    }
}
