package de.jxdev.legendarycraft.v3.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.argument.TeamArgument;
import de.jxdev.legendarycraft.v3.data.models.team.*;
import de.jxdev.legendarycraft.v3.exception.team.TeamServiceException;
import de.jxdev.legendarycraft.v3.util.CommandUtil;
import de.jxdev.legendarycraft.v3.util.TeamUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.NotImplementedException;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class TeamCommand {

    private final LegendaryCraft plugin = LegendaryCraft.getInstance();

    private final SimpleCommandExceptionType PREFIX_TOO_LONG = new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(
            Component.translatable("team.error.prefix_too_long", NamedTextColor.RED)
    ));
    private final SimpleCommandExceptionType NAME_TOO_LONG = new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(
            Component.translatable("team.error.name_too_long", NamedTextColor.RED)
    ));
    private final SimpleCommandExceptionType NAME_ALREADY_USED = new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(
            Component.translatable("team.error.name_already_used", NamedTextColor.RED)
    ));

    public LiteralCommandNode<CommandSourceStack> getCommand() {
        return Commands.literal("team")

                /* ---------- PUBLIC COMMANDS ---------- */
                .then(Commands.literal("info")
                        .then(Commands.argument("team", new TeamArgument())
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
                                .then(Commands.argument("color", ArgumentTypes.namedColor())
                                        .executes(this::teamCreateExecutor)
                                )
                                .executes(this::teamCreateExecutor)
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
                                        .executes(this::teamSettingsNameExecutor)
                                )
                        )
                        .then(Commands.literal("color")
                                .then(Commands.argument("color", ArgumentTypes.namedColor())
                                        .executes(this::teamSettingsColorExecutor)
                                )
                        )
                        .then(Commands.literal("prefix")
                                .then(Commands.argument("prefix", StringArgumentType.string())
                                        .executes(this::teamSettingsPrefixExecutor)
                                )
                        )
                )
                .then(Commands.literal("kick")
                        .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                        .then(Commands.argument("player", ArgumentTypes.playerProfiles())
                                .executes(this::teamKickExecutor)
                        )
                )

                /* ---------- TEAM MEMBERSHIPS ---------- */
                .then(Commands.literal("invite")
                        .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                        .then(Commands.argument("player", ArgumentTypes.playerProfiles())
                                .executes(this::teamInviteExecutor)
                        )
                )
                .then(Commands.literal("join")
                        .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                        .then(Commands.argument("team", new TeamArgument())
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
        TeamCacheRecord team = context.getArgument("team", TeamCacheRecord.class);

        List<TeamMember> memberList = plugin.getTeamService().getMemberList(team);

        Component response = Component.translatable("team.info.members", team.getChatComponent());
        for (TeamMember member : memberList) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(member.getPlayerId());
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
        List<TeamWithMemberCount> teams = plugin.getTeamService().getAllWithMemberCount();

        Component response = Component.translatable("team.info.list");

        for (TeamWithMemberCount team : teams) {
            response = response.append(Component.newline())
                    .append(Component.translatable("team.info.list_item", team.getChatComponent(), Component.text(team.getMemberCount())));
        }

        sender.sendMessage(response);
        return Command.SINGLE_SUCCESS;
    }

    private int teamCreateExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        String name = StringArgumentType.getString(context, "name");

        // Validate Team Membership \\
        TeamUtil.checkIfPlayerHasNoTeam(sender);

        NamedTextColor color = NamedTextColor.WHITE;
        try {
            color = context.getArgument("color", NamedTextColor.class);
        } catch (IllegalArgumentException e) {
            // Ignored, color is optional
        }

        try {
            Team team = plugin.getTeamService().createTeam(name, name, color, sender.getUniqueId());

            Component response = Component.translatable("team.success.create", team.getChatComponent()).color(NamedTextColor.GREEN);
            sender.sendMessage(response);
        } catch (TeamServiceException ex) {
            sender.sendMessage(ex.getChatComponent().color(NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int teamDeleteExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        TeamCacheRecord team = TeamUtil.getCurrentPlayerTeamFromCache(sender);
        TeamUtil.checkPlayerOwnsTeam(sender, team);

        try {
            this.plugin.getTeamService().deleteTeam(team);

            sender.sendMessage(Component.translatable("team.success.delete", team.getChatComponent())
                    .color(NamedTextColor.GREEN)
            );
        } catch (TeamServiceException ex) {
            sender.sendMessage(ex.getChatComponent().color(NamedTextColor.RED));
        }

        return Command.SINGLE_SUCCESS;
    }

    private int teamSettingsNameExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        String name = StringArgumentType.getString(context, "name");
        TeamCacheRecord team = TeamUtil.getCurrentPlayerTeamFromCache(sender);

        // Validate Permissions \\
        TeamUtil.checkPlayerOwnsTeam(sender, team);

        try {
            // Change Name \\
            plugin.getTeamService().setTeamName(team, name);

            sender.sendMessage(Component.translatable("team.success.settings.name", Component.text(name))
                    .color(NamedTextColor.GREEN)
            );
        } catch (TeamServiceException ex) {
            sender.sendMessage(ex.getChatComponent().color(NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int teamSettingsColorExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        NamedTextColor color = context.getArgument("color", NamedTextColor.class);
        TeamCacheRecord team = TeamUtil.getCurrentPlayerTeamFromCache(sender);

        // Validate Permissions \\
        TeamUtil.checkPlayerOwnsTeam(sender, team);

        try {
            // Change Color \\
            plugin.getTeamService().setTeamColor(team, color);

            sender.sendMessage(Component.translatable("team.success.settings.color", Component.text(color.toString()))
                    .color(NamedTextColor.GREEN)
            );
        } catch (TeamServiceException ex) {
            sender.sendMessage(ex.getChatComponent().color(NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int teamSettingsPrefixExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        String prefix = StringArgumentType.getString(context, "prefix");
        TeamCacheRecord team = TeamUtil.getCurrentPlayerTeamFromCache(sender);

        // Validate Permissions \\
        TeamUtil.checkPlayerOwnsTeam(sender, team);

        try {
            // Change Prefix \\
            plugin.getTeamService().setTeamPrefix(team, prefix);

            sender.sendMessage(Component.translatable("team.success.settings.color", Component.text(prefix))
                    .color(NamedTextColor.GREEN)
            );
        } catch (TeamServiceException ex) {
            sender.sendMessage(ex.getChatComponent().color(NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int teamKickExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        TeamCacheRecord team = TeamUtil.getCurrentPlayerTeamFromCache(sender);
        TeamUtil.checkPlayerOwnsTeam(sender, team);

        final PlayerProfileListResolver targetResolver = context.getArgument("player", PlayerProfileListResolver.class);
        final Collection<PlayerProfile> targetProfiles = targetResolver.resolve(context.getSource());
        final var playerProfile = targetProfiles.iterator().next();

        try {
            plugin.getTeamService().removePlayerFromTeam(team, playerProfile.getId());

            sender.sendMessage(Component.translatable("team.success.kick",
                    Component.text(Objects.requireNonNullElse(playerProfile.getName(), "UNKNOWN")))
                    .color(NamedTextColor.GREEN)
            );
        } catch (TeamServiceException ex) {
            sender.sendMessage(ex.getChatComponent().color(NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int teamInviteExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        TeamCacheRecord team = TeamUtil.getCurrentPlayerTeamFromCache(sender);

        final PlayerProfileListResolver targetResolver = context.getArgument("player", PlayerProfileListResolver.class);
        final Collection<PlayerProfile> targetProfiles = targetResolver.resolve(context.getSource());
        final var playerProfile = targetProfiles.iterator().next();

        if (playerProfile == null)
            throw new IllegalArgumentException("Player is null");
        if (playerProfile.getId() == null)
            throw new IllegalArgumentException("Player UUID is null");

        try {
            // Initialize Team Invite \\
            plugin.getTeamService().invitePlayerToTeam(team, playerProfile.getId());

            sender.sendMessage(Component.translatable("team.success.invite",
                            Component.text(Objects.requireNonNullElse(playerProfile.getName(), playerProfile.getId().toString()))
                    ).color(NamedTextColor.GREEN)
            );
        } catch (TeamServiceException ex) {
            sender.sendMessage(ex.getChatComponent().color(NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int teamJoinExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        TeamCacheRecord team = context.getArgument("team", TeamCacheRecord.class);

        try {
            plugin.getTeamService().acceptInvite(team, sender.getUniqueId());

            sender.sendMessage(Component.translatable("team.success.join", team.getChatComponent())
                    .color(NamedTextColor.GREEN)
            );
        } catch (TeamServiceException ex) {
            sender.sendMessage(ex.getChatComponent().color(NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int teamLeaveExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        TeamCacheRecord team = TeamUtil.getCurrentPlayerTeamFromCache(sender);

        if (plugin.getTeamService().isPlayerTeamOwner(sender.getUniqueId(), team)) {
            sender.sendMessage(Component.translatable("team.error.cannot_leave_owner").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        try {
            plugin.getTeamService().removePlayerFromTeam(team, sender.getUniqueId());

            sender.sendMessage(Component.translatable("team.success.leave", team.getChatComponent())
                    .color(NamedTextColor.GREEN)
            );
        } catch (TeamServiceException ex) {
            sender.sendMessage(ex.getChatComponent().color(NamedTextColor.RED));
        }

        return Command.SINGLE_SUCCESS;
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
