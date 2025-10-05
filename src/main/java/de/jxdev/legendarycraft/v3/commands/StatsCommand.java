package de.jxdev.legendarycraft.v3.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.argument.OfflinePlayerArgument;
import de.jxdev.legendarycraft.v3.service.PlayerStatsService;
import de.jxdev.legendarycraft.v3.service.TeamService;
import de.jxdev.legendarycraft.v3.util.CommandUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;

import java.util.Collection;
import java.util.Objects;

public class StatsCommand {
    private final LegendaryCraft plugin = LegendaryCraft.getInstance();

    public LiteralCommandNode<CommandSourceStack> getCommand() {
        return Commands.literal("stats")
                .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                .executes(this::statsExecutor)
                .then(Commands.argument("player", new OfflinePlayerArgument())
                        .executes(this::statsExecutor)
                )
                .build();
    }

    private int statsExecutor(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        OfflinePlayer targetPlayer;

        try {
            targetPlayer = context.getArgument("player", OfflinePlayer.class);
        } catch (Exception e) {
            targetPlayer = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        }

        PlayerStatsService stats = plugin.getPlayerStatsService();
        TeamService teamService = plugin.getTeamService();

        long playtimeMs = stats.getTotalPlaytimeMsIncludingActive(targetPlayer.getUniqueId());
        String playtimeFormatted = PlayerStatsService.formatDuration(playtimeMs);
        int joinCount = stats.getJoinCount(targetPlayer.getUniqueId());
        var team = teamService.getTeamByPlayer(targetPlayer.getUniqueId());

        Component response = Component.translatable("stats.info.stats_of", Component.text(Objects.requireNonNullElse(targetPlayer.getName(), targetPlayer.getUniqueId().toString())).color(NamedTextColor.AQUA))
                .append(Component.newline())
                .append(Component.text("- "))
                .append(Component.translatable("stats.info.playtime", Component.text(playtimeFormatted).color(NamedTextColor.AQUA)))
                .append(Component.newline())
                .append(Component.text("- "))
                .append(Component.translatable("stats.info.joins", Component.text(String.valueOf(joinCount)).color(NamedTextColor.AQUA)));

        if (team.isPresent()) {
            response = response.append(Component.newline());
            response = response.append(Component.text("- "));
            response = response.append(Component.translatable("stats.info.team", team.get().getChatComponent()));
        }

        context.getSource().getSender().sendMessage(response);
        return Command.SINGLE_SUCCESS;
    }
}
