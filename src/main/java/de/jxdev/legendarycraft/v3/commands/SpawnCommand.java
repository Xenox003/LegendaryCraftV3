package de.jxdev.legendarycraft.v3.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.service.SpawnDebuffService;
import de.jxdev.legendarycraft.v3.util.CommandUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SpawnCommand {
    public LiteralCommandNode<CommandSourceStack> getCommand() {
        return Commands.literal("spawn")
                .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                .executes(this::executeSpawn)
                .build();
    }

    private static final String BYPASS_PERMISSION = "legendarycraft.spawn.bypass";

    private int executeSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());

        SpawnDebuffService debuffs = LegendaryCraft.getInstance().getSpawnDebuffService();

        if (!player.hasPermission(BYPASS_PERMISSION)) {
            if (debuffs.isInCombat(player)) {
                long remain = debuffs.getCombatRemainingSeconds(player);
                player.sendMessage(Component.translatable("spawn.error.in_combat", Component.text(String.valueOf(remain))).color(NamedTextColor.RED));
                return 0;
            }
            if (debuffs.isOnCooldown(player)) {
                long remain = debuffs.getCooldownRemainingSeconds(player);
                player.sendMessage(Component.translatable("spawn.error.cooldown", Component.text(String.valueOf(remain))).color(NamedTextColor.RED));
                return 0;
            }
        }

        Location spawn = player.getWorld().getSpawnLocation();
        player.teleportAsync(spawn);
        debiffsSafeRecord(debuffs, player);
        return Command.SINGLE_SUCCESS;
    }

    private void debiffsSafeRecord(SpawnDebuffService debuffs, Player player) {
        if (debuffs == null) return;
        if (player.hasPermission(BYPASS_PERMISSION)) return;
        debuffs.recordSpawnUse(player);
    }
}
