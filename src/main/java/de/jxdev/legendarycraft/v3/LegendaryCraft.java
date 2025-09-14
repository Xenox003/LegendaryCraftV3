package de.jxdev.legendarycraft.v3;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jxdev.legendarycraft.v3.commands.TeamCommand;
import de.jxdev.legendarycraft.v3.db.Database;
import de.jxdev.legendarycraft.v3.service.TeamService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

@Getter
public final class LegendaryCraft extends JavaPlugin {

    public LegendaryCraft() {
        LegendaryCraft.setInstance(this);
    }

    @Setter
    @Getter
    private static LegendaryCraft instance;

    private Database database;
    private TeamService teamService;

    @Override
    public void onEnable() {
        // Ensure config exists \
        saveDefaultConfig();

        try {
            // Initialize Database using data folder + config value
            String dbFileName = getConfig().getString("database.file", "storage.db");
            Path dbPath = getDataFolder().toPath().resolve(dbFileName);
            this.database = new Database(dbPath);
            this.database.init();

            // Create and load repository cache
            this.teamService = new TeamService(database);
            this.teamService.loadAll();
        } catch (Exception e) {
            getLogger().severe("Failed to initialize database/repository: " + e.getMessage());
            e.printStackTrace();
            // Disable plugin if critical
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register Commands (use repository)
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralCommandNode<CommandSourceStack> teamCommand = new TeamCommand().getCommand();
            commands.registrar().register(teamCommand);
        });

        getLogger().info("Plugin initialized.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled.");
    }

}
