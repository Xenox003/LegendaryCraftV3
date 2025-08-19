package de.jxdev.legendarycraft.v3.commands;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChestCommand implements TabExecutor {
    private List<String> subCommands;

    public ChestCommand() {
        subCommands = List.of("lock", "unlock");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        var messages = LegendaryCraft.getInstance().getMessages();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("commands.error.player-only"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(messages.msg("commands.chest.usage"));
            return true;
        }

        // TODO: player needs to be in Team

        var targetBlock = player.getTargetBlock(null, 5);
        if (targetBlock.isEmpty() || (targetBlock.getType() != Material.CHEST && targetBlock.getType() != Material.TRAPPED_CHEST && targetBlock.getType() != Material.BARREL)) {
            player.sendMessage(messages.msg("commands.chest.no-target"));
            return true;
        }

        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length <= 1) {
            return subCommands;
        }
        return List.of();
    }
}
