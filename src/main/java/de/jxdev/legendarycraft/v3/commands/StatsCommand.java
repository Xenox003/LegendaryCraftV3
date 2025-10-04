package de.jxdev.legendarycraft.v3.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.service.PlayerStatsService;
import de.jxdev.legendarycraft.v3.service.TeamService;
import de.jxdev.legendarycraft.v3.util.CommandUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collection;
import java.util.Objects;

public class StatsCommand {
    private final LegendaryCraft plugin = LegendaryCraft.getInstance();

    public LiteralCommandNode<CommandSourceStack> getCommand() {
        return Commands.literal("stats")
                .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                .executes(this::statsExecutor)
                .then(Commands.argument("player", ArgumentTypes.playerProfiles())
                        .executes(this::statsExecutor)
                )
                .build();
    }

    private int statsExecutor(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PlayerProfile targetProfile;

        try {
            final PlayerProfileListResolver targetResolver = context.getArgument("player", PlayerProfileListResolver.class);
            final Collection<PlayerProfile> targetProfiles = targetResolver.resolve(context.getSource());
            targetProfile = targetProfiles.iterator().next();
        } catch (Exception e) {
            targetProfile = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender()).getPlayerProfile();
        }

        if (targetProfile.getId() == null) throw new IllegalArgumentException("Player UUID is null");

        PlayerStatsService stats = plugin.getPlayerStatsService();
        TeamService teamService = plugin.getTeamService();

        long playtimeMs = stats.getTotalPlaytimeMsIncludingActive(targetProfile.getId());
        String playtimeFormatted = PlayerStatsService.formatDuration(playtimeMs);
        int joinCount = stats.getJoinCount(targetProfile.getId());
        var team = teamService.getTeamByPlayer(targetProfile.getId());

        Component response = Component.translatable("stats.info.stats_of", Component.text(Objects.requireNonNullElse(targetProfile.getName(), targetProfile.getId().toString())).color(NamedTextColor.AQUA))
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
