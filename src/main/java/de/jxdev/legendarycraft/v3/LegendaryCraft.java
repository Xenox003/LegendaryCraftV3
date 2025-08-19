package de.jxdev.legendarycraft.v3;

import de.jxdev.legendarycraft.v3.commands.ChestCommand;
import de.jxdev.legendarycraft.v3.commands.TeamCommand;
import de.jxdev.legendarycraft.v3.framework.CommandManager;
import de.jxdev.legendarycraft.v3.i18n.Messages;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class LegendaryCraft extends JavaPlugin {

    private static LegendaryCraft instance;
    private Messages messages;
    private CommandManager commandManager;

    public static LegendaryCraft getInstance() {
        return instance;
    }

    public Messages getMessages() {
        return messages;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Save default config (will include language setting once provided)
        saveDefaultConfig();

        // Initialize messages system
        this.messages = new Messages(this);
        this.messages.init();

        // Initialize Command Manager
        this.commandManager = new CommandManager(this);

        // Plugin startup logic

        // Register Commands
        registerCommandAndTabCompleter("team", new TeamCommand());
        registerCommandAndTabCompleter("chest", new ChestCommand());
    }

    private void registerCommandAndTabCompleter(String name, TabExecutor executor) {
        var command = this.getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command '" + name + "' has not been properly added to the plugin.yml");
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
