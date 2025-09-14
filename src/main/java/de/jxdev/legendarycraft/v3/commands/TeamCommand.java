package de.jxdev.legendarycraft.v3.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
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
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.NotImplementedException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Predicate;

public class TeamCommand {

    private static final SimpleCommandExceptionType PLAYER_NOT_IN_TEAM = new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(
            Component.text("Du kannst diesen Befehl nur nutzen, wenn du in einem Team bist!", NamedTextColor.RED)
    ));
    private static final DynamicCommandExceptionType PLAYER_DOESNT_OWN_TEAM = new DynamicCommandExceptionType(team -> MessageComponentSerializer.message().serialize(
            Component.text("Du kannst diesen Befehl nur nutzen, wenn du der Besitzer des Team ")
                    .append(((Team)team).getChatComponent())
                    .append(Component.text(" bist!"))
                    .color(NamedTextColor.RED)
    ));
    private static final DynamicCommandExceptionType PLAYER_ALREADY_IN_TEAM = new DynamicCommandExceptionType(team -> MessageComponentSerializer.message().serialize(
            Component.text("Du kannst diesen Befehl nur nutzen, wenn du aktuell in keinem Team bist!\n")
                    .append(Component.text("Im Moment bist du noch Mitglied von "))
                    .append(((Team)team).getChatComponent())
                    .append(Component.text("!"))
                    .color(NamedTextColor.RED)
    ));

    private final Predicate<CommandSourceStack> PLAYER_ONLY =
            src -> src.getSender() instanceof Player;

    /**
     * Gets the current player's team
     * @return the current player's team
     * @throws CommandSyntaxException when the player is not in a team
     */
    private Team getCurrentPlayerTeam(Player player) throws CommandSyntaxException {
        Optional<Team> team = this.plugin.getTeamService().getPlayerTeam(player.getUniqueId());
        if (team.isEmpty()) {
            throw PLAYER_NOT_IN_TEAM.create();
        }

        return team.get();
    }

    /**
     * Checks if the given player has no team.
     * @throws CommandSyntaxException when the player already has a team
     */
    private void checkIfPlayerHasNoTeam(Player player) throws CommandSyntaxException {
        Optional<Team> team = this.plugin.getTeamService().getPlayerTeam(player.getUniqueId());
        if (team.isPresent()) {
            throw PLAYER_ALREADY_IN_TEAM.create(team.get());
        }
    }

    /**
     * Checks if the given player owns the given team.
     * @throws CommandSyntaxException when the player doesn't own the team
     */
    private void checkPlayerOwnsTeam(Player player, Team team) throws CommandSyntaxException {
        TeamMemberRole role = team.getMembers().get(player.getUniqueId());
        if (role != TeamMemberRole.OWNER) {
            throw PLAYER_DOESNT_OWN_TEAM.create(team);
        }
    }

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
                        .requires(PLAYER_ONLY)
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
                        .requires(PLAYER_ONLY)
                        .executes(this::teamDeleteExecutor)
                )
                .then(Commands.literal("settings")
                        .requires(PLAYER_ONLY)
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
                        .requires(PLAYER_ONLY)
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(this::teamKickExecutor)
                        )
                )

                /* ---------- TEAM MEMBERSHIPS ---------- */
                .then(Commands.literal("invite")
                        .requires(PLAYER_ONLY)
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(this::teamInviteExecutor)
                        )
                )
                .then(Commands.literal("join")
                        .requires(PLAYER_ONLY)
                        .then(Commands.argument("team", new TeamArgument(plugin.getTeamService()))
                                .executes(this::teamJoinExecutor)
                        )
                )
                .then(Commands.literal("leave")
                        .requires(PLAYER_ONLY)
                        .executes(this::teamLeaveExecutor)
                )

                /* ---------- PUBLIC CHAT ---------- */
                .then(Commands.literal("chat")
                        .requires(PLAYER_ONLY)
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(this::teamChatExecutor)
                        ))
                .then(Commands.literal("chat_toggle")
                        .requires(PLAYER_ONLY)
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


        throw new NotImplementedException();
    }

    private int teamListExecutor(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        throw new NotImplementedException();
    }

    private int teamCreateExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = (Player) context.getSource().getSender();
        String name = StringArgumentType.getString(context, "name");
        String prefix = StringArgumentType.getString(context, "prefix");

        checkIfPlayerHasNoTeam(sender);

        NamedTextColor color = null;
        try {
            color = context.getArgument("color", NamedTextColor.class);
        } catch (IllegalArgumentException e) {
            // Ignored, color is optional
        }

        Team team = null;
        try {
            team = plugin.getTeamService().createTeam(name, prefix, sender.getUniqueId(), color);

            Component response = Component.text("Team erstellt: ")
                    .append(team.getChatComponent());
            sender.sendMessage(response);
            return Command.SINGLE_SUCCESS;
        } catch (SQLException e) {
            Component errorResponse = Component.text("Interner Fehler beim erstellen des Teams!", NamedTextColor.RED);
            sender.sendMessage(errorResponse);

            plugin.getLogger().severe("Failed to create team: " + e.getMessage());

            return 0;
        }
    }

    private int teamDeleteExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = (Player) context.getSource().getSender();
        Team team = getCurrentPlayerTeam(sender);
        checkPlayerOwnsTeam(sender, team);

        try {
            this.plugin.getTeamService().deleteTeam(team);

            sender.sendMessage(Component.text("Das Team ")
                    .append(team.getChatComponent())
                    .append(Component.text(" wurde erfolgreich gelöscht"))
                    .color(NamedTextColor.GREEN)
            );

            return Command.SINGLE_SUCCESS;
        } catch (SQLException e) {
            Component errorResponse = Component.text("Interner Fehler beim löschen des Teams!", NamedTextColor.RED);
            sender.sendMessage(errorResponse);

            plugin.getLogger().severe("Failed to delete team: " + e.getMessage());

            return 0;
        }
    }

    private int teamSettingsNameExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = (Player) context.getSource().getSender();

        throw new NotImplementedException();
    }

    private int teamSettingsColorExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = (Player) context.getSource().getSender();

        throw new NotImplementedException();
    }

    private int teamSettingsPrefixExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = (Player) context.getSource().getSender();

        throw new NotImplementedException();
    }

    private int teamKickExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        throw new NotImplementedException();
    }

    private int teamInviteExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = (Player) context.getSource().getSender();

        throw new NotImplementedException();
    }

    private int teamJoinExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = (Player) context.getSource().getSender();

        throw new NotImplementedException();
    }

    private int teamLeaveExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = (Player) context.getSource().getSender();

        throw new NotImplementedException();
    }

    private int teamChatExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = (Player) context.getSource().getSender();

        throw new NotImplementedException();
    }

    private int teamChatToggleExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player sender = (Player) context.getSource().getSender();

        throw new NotImplementedException();
    }

}
