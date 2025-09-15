package de.jxdev.legendarycraft.v3.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.models.Team;
import de.jxdev.legendarycraft.v3.models.TeamMemberRole;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;

public class TeamCommandUtil {
    private static final LegendaryCraft plugin = LegendaryCraft.getInstance();

    private static final SimpleCommandExceptionType PLAYER_NOT_IN_TEAM = new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(
            Component.translatable("team.error.member_required", NamedTextColor.RED)
    ));
    private static final DynamicCommandExceptionType PLAYER_DOESNT_OWN_TEAM = new DynamicCommandExceptionType(team -> MessageComponentSerializer.message().serialize(
            Component.translatable("team.error.no_owner", ((Team)team).getChatComponent())
                    .color(NamedTextColor.RED)
    ));
    private static final DynamicCommandExceptionType PLAYER_ALREADY_IN_TEAM = new DynamicCommandExceptionType(team -> MessageComponentSerializer.message().serialize(
            Component.translatable("team.error.no_member_required")
                    .append(Component.text("\n"))
                    .append(Component.translatable("team.info.current_team", ((Team)team).getChatComponent()))
                    .color(NamedTextColor.RED)
    ));

    /**
     * Gets the current player's team
     * @return the current player's team
     * @throws CommandSyntaxException when the player is not in a team
     */
    public static Team getCurrentPlayerTeam(Player player) throws CommandSyntaxException {
        Optional<Team> team = plugin.getTeamService().getPlayerTeam(player.getUniqueId());
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
        Optional<Team> team = plugin.getTeamService().getPlayerTeam(player.getUniqueId());
        if (team.isPresent()) {
            throw PLAYER_ALREADY_IN_TEAM.create(team.get());
        }
    }

    /**
     * Checks if the given player owns the given team.
     * @throws CommandSyntaxException when the player doesn't own the team
     */
    public static void checkPlayerOwnsTeam(Player player, Team team) throws CommandSyntaxException {
        TeamMemberRole role = team.getMembers().get(player.getUniqueId());
        if (role != TeamMemberRole.OWNER) {
            throw PLAYER_DOESNT_OWN_TEAM.create(team);
        }
    }
}
