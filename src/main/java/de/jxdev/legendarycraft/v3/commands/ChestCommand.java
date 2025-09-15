package de.jxdev.legendarycraft.v3.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.models.LockedChest;
import de.jxdev.legendarycraft.v3.models.Team;
import de.jxdev.legendarycraft.v3.util.CommandUtil;
import de.jxdev.legendarycraft.v3.util.TeamCommandUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import java.sql.SQLException;
import java.util.Optional;

public class ChestCommand {
    private final LegendaryCraft plugin = LegendaryCraft.getInstance();

    private static final SimpleCommandExceptionType NO_TARGET_ERROR = new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(
            Component.translatable("chest.error.no_chest_in_sight", NamedTextColor.RED)
    ));
    private static final DynamicCommandExceptionType ALREADY_LOCKED_ERROR = new DynamicCommandExceptionType(team -> MessageComponentSerializer.message().serialize(
            Component.translatable("chest.error.already_locked", ((Team)team).getChatComponent())
                    .color(NamedTextColor.RED)
    ));
    private static final SimpleCommandExceptionType NOT_LOCKED_ERROR = new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(
            Component.translatable("chest.error.not_locked", NamedTextColor.RED)
    ));
    private static final SimpleCommandExceptionType NOT_IN_TEAM_ERROR = new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(
            Component.translatable("chest.error.not_in_team", NamedTextColor.RED)
    ));
    private static final SimpleCommandExceptionType NOT_OWNER_ERROR = new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(
            Component.translatable("chest.error.not_owner", NamedTextColor.RED)
    ));
    private static final DynamicCommandExceptionType LIMIT_REACHED = new DynamicCommandExceptionType(limit -> MessageComponentSerializer.message().serialize(
            Component.translatable("chest.error.limit_reached", Component.text(String.valueOf(limit)))
                    .color(NamedTextColor.RED)
    ));

    public LiteralCommandNode<CommandSourceStack> getCommand() {
        return Commands.literal("chest")
                .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                .then(Commands.literal("lock")
                        .executes(this::chestLockExecutor)
                )
                .then(Commands.literal("unlock")
                        .executes(this::chestUnlockExecutor)
                )
                .then(Commands.literal("list")
                        .executes(this::chestListExecutor))
                .build();
    }

    private int chestLockExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        Team team = TeamCommandUtil.getCurrentPlayerTeam(player);
        Block target = player.getTargetBlock(null, 8);

        // Validate target is a Chest \\
        if (target.getType() != Material.CHEST) throw NO_TARGET_ERROR.create();

        // Validate if the chest is locked \\
        Optional<LockedChest> lockedChest = plugin.getChestService().getLockedChest(target);
        if (lockedChest.isPresent()) {
            Team ownerTeam = plugin.getTeamService().getById(lockedChest.get().getTeamId()).orElseThrow(() -> new IllegalStateException("Could not find owner team of already locked chest!"));
            throw ALREADY_LOCKED_ERROR.create(ownerTeam);
        }

        // Enforce Limit \\
        if (!plugin.getChestService().checkLockLimit(team)) {
            throw LIMIT_REACHED.create(plugin.getChestService().getLockLimit(team));
        }

        // Lock Chest \\
        try {
            plugin.getChestService().lockChest(team, target);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to lock chest: " + e.getMessage());
            throw CommandUtil.INTERNAL_ERROR.create();
        }

        player.sendMessage(Component.translatable("chest.success.lock"));
        return Command.SINGLE_SUCCESS;
    }

    private int chestUnlockExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        Team team = TeamCommandUtil.getCurrentPlayerTeam(player);
        Block target = player.getTargetBlock(null, 8);

        // Validate target is a Chest \\
        if (target.getType() != Material.CHEST) throw NO_TARGET_ERROR.create();

        // Validate if the chest is locked \\
        Optional<LockedChest> lockedChest = plugin.getChestService().getLockedChest(target);
        if (lockedChest.isEmpty()) throw NOT_LOCKED_ERROR.create();

        // Validate if chest is owned by current team \\
        if (lockedChest.get().getTeamId() != team.getId()) throw NOT_OWNER_ERROR.create();

        // Unlock Chest \\
        try {
            plugin.getChestService().unlockChest(lockedChest.get());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unlock chest: " + e.getMessage());
            throw CommandUtil.INTERNAL_ERROR.create();
        }

        player.sendMessage(Component.translatable("chest.success.unlock"));
        return Command.SINGLE_SUCCESS;
    }

    private int chestListExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        Team team = TeamCommandUtil.getCurrentPlayerTeam(player);

        // TODO

        return Command.SINGLE_SUCCESS;
    }
}
