package de.jxdev.legendarycraft.v3.framework;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.commands.ChestCommand;
import de.jxdev.legendarycraft.v3.framework.models.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CommandManager implements TabExecutor {
    public List<Command> commands;
    private final LegendaryCraft plugin;

    public CommandManager(LegendaryCraft plugin) {
        commands = List.of();
        this.plugin = plugin;
    }

    public void registerCommand(Command command) {
        org.bukkit.command.PluginCommand pluginCommand = plugin.getCommand(command.name);

        if (pluginCommand == null) {
            throw new IllegalStateException("Command '" + command.name + "' has not been properly added to the plugin.yml");
        }

        // Register Aliases (if needed)
        if (command.aliases != null) {
            pluginCommand.setAliases(command.aliases);
        }

        // Register Handler and TabCompleter
        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        Command cmd = commands.stream().filter(c -> c.name.equalsIgnoreCase(command.getName())).findFirst().orElse(null);

        if (cmd == null) {
            throw new IllegalStateException("Command '" + command.getName() + "' has not been properly registered in CommandManager");
        }

        cmd.execute(sender, args);

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
