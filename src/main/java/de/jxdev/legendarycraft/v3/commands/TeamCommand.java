package de.jxdev.legendarycraft.v3.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.argument.TeamArgument;
import de.jxdev.legendarycraft.v3.entities.Team;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.NotImplementedException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class TeamCommand {

    private LegendaryCraft plugin;

    public TeamCommand() {
        this.plugin = LegendaryCraft.getInstance();
    }

    public LiteralCommandNode<CommandSourceStack> getCommand() {
        return Commands.literal("team")

                /* ---------- PUBLIC COMMANDS ---------- */
                .then(Commands.literal("info")
                        .then(Commands.argument("team", new TeamArgument(plugin.getTeamRepository()))
                                .executes(this::teamInfoExecutor)
                        )
                )
                .then(Commands.literal("list")
                        .executes(this::teamListExecutor)
                )

                /* ---------- TEAM MANAGEMENT ---------- */
                .then(Commands.literal("create")
                        .requires(source -> source.getSender() instanceof Player)
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
                        .requires(source -> source.getSender() instanceof Player)
                        // TODO: require team membership with level OWNER
                        .executes(this::teamDeleteExecutor)
                )
                .then(Commands.literal("settings")
                        .requires(source -> source.getSender() instanceof Player)
                        // TODO: require team membership with level OWNER
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
                        .requires(source -> source.getSender() instanceof Player)
                        // TODO: require team membership with level OWNER
                )

                /* ---------- TEAM MEMBERSHIPS ---------- */
                .then(Commands.literal("invite")
                        .requires(source -> source.getSender() instanceof Player)
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(this::teamInviteExecutor)
                        )
                )
                .then(Commands.literal("join")
                        .requires(source -> source.getSender() instanceof Player)
                        .then(Commands.argument("team", new TeamArgument(plugin.getTeamRepository()))
                                .executes(this::teamJoinExecutor)
                        )
                )
                .then(Commands.literal("leave")
                        .requires(source -> source.getSender() instanceof Player)
                        .executes(this::teamLeaveExecutor)
                )

                /* ---------- PUBLIC CHAT ---------- */
                .then(Commands.literal("chat")
                        .requires(source -> source.getSender() instanceof Player)
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(this::teamChatExecutor)
                        ))
                .then(Commands.literal("chat_toggle")
                        .requires(source -> source.getSender() instanceof Player)
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

    private int teamCreateExecutor(CommandContext<CommandSourceStack> context) {
        Player sender = (Player) context.getSource().getSender();
        String name = StringArgumentType.getString(context, "name");
        String prefix = StringArgumentType.getString(context, "prefix");

        Team team = null;
        try {
            team = plugin.getTeamRepository().createTeam(name, prefix, "white", sender.getUniqueId());

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

    private int teamDeleteExecutor(CommandContext<CommandSourceStack> context) {
        Player sender = (Player) context.getSource().getSender();

        throw new NotImplementedException();
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
