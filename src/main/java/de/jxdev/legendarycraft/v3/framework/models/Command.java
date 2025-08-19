package de.jxdev.legendarycraft.v3.framework.models;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Command {

    // General Parameters
    /**
     * The name of the command.
     */
    public String name;

    /**
     * The description of the command.
     */
    public String description;

    /**
     * Possible aliases for the command.
     */
    public List<String> aliases;

    // Access Control
    /**
     * Permission node required to execute the command.
     */
    public String permission;

    /**
     * Whether the command can only be executed by players.
     */
    public boolean playerOnly;

    /**
     * Whether the command can only be executed by the console.
     */
    public boolean consoleOnly;

    // Command Execution
    /**
     * List of sub-commands. (Cannot be used with arguments.)
     */
    public List<Command> subCommands;

    /**
     * List of arguments. (Cannot be used with sub-commands.)
     */
    public List<String> arguments;

    public void preExecute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (this.subCommands != null) {

        }
    }

    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {

    }
}
