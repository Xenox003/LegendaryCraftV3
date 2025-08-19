package de.jxdev.legendarycraft.v3.commands;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TeamCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Example usage of the messages API
        var messages = LegendaryCraft.getInstance().getMessages();
        if (sender instanceof Player player) {
            player.sendMessage(messages.msg("commands.team.hello",
                    "player", player.getName(),
                    "prefix", messages.msg("prefix")));
        } else {
            sender.sendMessage(messages.msg("commands.team.hello",
                    "player", sender.getName(),
                    "prefix", messages.msg("prefix")));
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of("test", "1", "2");
    }
}
