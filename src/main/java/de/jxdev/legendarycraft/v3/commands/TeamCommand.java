package de.jxdev.legendarycraft.v3.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.argument.TeamArgument;
import de.jxdev.legendarycraft.v3.models.Team;
import de.jxdev.legendarycraft.v3.models.TeamMemberRole;
import de.jxdev.legendarycraft.v3.util.CommandUtil;
import de.jxdev.legendarycraft.v3.util.TeamCommandUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.NotImplementedException;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class TeamCommand {

    private final LegendaryCraft plugin = LegendaryCraft.getInstance();

    public LiteralCommandNode<CommandSourceStack> getCommand() {
        return Commands.literal("team")

                /* ---------- PUBLIC COMMANDS ---------- */
                .then(Commands.literal("info")
                        .then(Commands.argument("team", new TeamArgument(plugin.getTeamService()))
                                .executes(this::teamInfoExecutor)
                        )
                )
                .then(Commands.literal("list")
                        .executes(this::teamListExecutor)
                )

                /* ---------- TEAM MANAGEMENT ---------- */
                .then(Commands.literal("create")
                        .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("prefix", StringArgumentType.string())
                                    .executes(this::teamCreateExecutor)
                                    .then(Commands.argument("color", ArgumentTypes.namedColor())
                                            .executes(this::teamCreateExecutor)
                                    )
                                )
                        )
                )
                .then(Commands.literal("delete")
                        .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                        .executes(this::teamDeleteExecutor)
                )
                .then(Commands.literal("settings")
                        .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                        .then(Commands.literal("name")
                                .then(Commands.argument("name", StringArgumentType.string())
                                )
                        )
                        .then(Commands.literal("color")
                                .then(Commands.argument("color", ArgumentTypes.namedColor()))
                        )
                        .then(Commands.literal("prefix")
                                .then(Commands.argument("prefix", StringArgumentType.string()))
                        )
                )
                .then(Commands.literal("kick")
                        .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(this::teamKickExecutor)
                        )
                )

                /* ---------- TEAM MEMBERSHIPS ---------- */
                .then(Commands.literal("invite")
                        .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(this::teamInviteExecutor)
                        )
                )
                .then(Commands.literal("join")
                        .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                        .then(Commands.argument("team", new TeamArgument(plugin.getTeamService()))
                                .executes(this::teamJoinExecutor)
                        )
                )
                .then(Commands.literal("leave")
                        .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                        .executes(this::teamLeaveExecutor)
                )

                /* ---------- PUBLIC CHAT ---------- */
                .then(Commands.literal("chat")
                        .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(this::teamChatExecutor)
                        ))
                .then(Commands.literal("chat_toggle")
                        .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                )
                /*
                .then(Commands.literal("admin")
                        .requires(source -> source.getSender().hasPermission("lc.team.admin"))
                        .then(Commands.literal("delete"))
                        .then(Commands.literal("join"))
                        .then(Commands.literal("kick"))
                )
                */
                .build();
    }

    private int teamInfoExecutor(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        Team team = context.getArgument("team", Team.class);

        Component response = Component.translatable("team.info.members", team.getChatComponent());

        for (UUID memberId : team.getMembers().keySet()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(memberId);
            String playerName = (player.getName() != null)
                    ? player.getName()
                    : player.getUniqueId().toString();
            response = response.append(Component.newline())
                    .append(Component.text("- " + playerName, NamedTextColor.GRAY));
        }

        sender.sendMessage(response);
        return Command.SINGLE_SUCCESS;
    }

    private int teamListExecutor(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        List<Team> teams = plugin.getTeamService().getAll();

        Component response = Component.translatable("team.info.list");

        for (Team team : teams) {
            response = response.append(Component.newline())
                    .append(Component.translatable("team.info.list_item", team.getChatComponent(), Component.text(team.getMembers().size())));
        }

        sender.sendMessage(response);
        return Command.SINGLE_SUCCESS;
    }

    private int teamCreateExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        String name = StringArgumentType.getString(context, "name");
        String prefix = StringArgumentType.getString(context, "prefix");

        TeamCommandUtil.checkIfPlayerHasNoTeam(sender);

        NamedTextColor color = null;
        try {
            color = context.getArgument("color", NamedTextColor.class);
        } catch (IllegalArgumentException e) {
            // Ignored, color is optional
        }

        try {
            Team team = plugin.getTeamService().createTeam(name, prefix, sender.getUniqueId(), color);

            Component response = Component.translatable("team.success.create", team.getChatComponent()).color(NamedTextColor.GREEN);
            sender.sendMessage(response);
            return Command.SINGLE_SUCCESS;
        } catch (SQLException e) {
            Component errorResponse = Component.translatable("common.error.internal_error", NamedTextColor.RED);
            sender.sendMessage(errorResponse);

            plugin.getLogger().severe("Failed to create team: " + e.getMessage());

            return 0;
        }
    }

    private int teamDeleteExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        Team team = TeamCommandUtil.getCurrentPlayerTeam(sender);
        TeamCommandUtil.checkPlayerOwnsTeam(sender, team);

        try {
            this.plugin.getTeamService().deleteTeam(team);

            sender.sendMessage(Component.translatable("team.success.delete", team.getChatComponent())
                    .color(NamedTextColor.GREEN)
            );

            return Command.SINGLE_SUCCESS;
        } catch (SQLException e) {
            Component errorResponse = Component.translatable("common.error.internal_error", NamedTextColor.RED);
            sender.sendMessage(errorResponse);

            plugin.getLogger().severe("Failed to delete team: " + e.getMessage());

            return 0;
        }
    }

    private int teamSettingsNameExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());

        throw new NotImplementedException();
    }

    private int teamSettingsColorExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());

        throw new NotImplementedException();
    }

    private int teamSettingsPrefixExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());

        throw new NotImplementedException();
    }

    private int teamKickExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());

        throw new NotImplementedException();
    }

    private int teamInviteExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());

        throw new NotImplementedException();
    }

    private int teamJoinExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());

        throw new NotImplementedException();
    }

    private int teamLeaveExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());

        throw new NotImplementedException();
    }

    private int teamChatExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());

        throw new NotImplementedException();
    }

    private int teamChatToggleExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());

        throw new NotImplementedException();
    }

}
