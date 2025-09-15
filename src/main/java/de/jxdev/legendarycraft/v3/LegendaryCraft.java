package de.jxdev.legendarycraft.v3;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jxdev.legendarycraft.v3.commands.TeamCommand;
import de.jxdev.legendarycraft.v3.db.Database;
import de.jxdev.legendarycraft.v3.events.PlayerJoinListener;
import de.jxdev.legendarycraft.v3.playerlist.PlayerListComponents;
import de.jxdev.legendarycraft.v3.service.ChestService;
import de.jxdev.legendarycraft.v3.service.PlayerNameService;
import de.jxdev.legendarycraft.v3.service.TeamService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.block.Chest;
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
    private ChestService chestService;
    private PlayerNameService playerNameService;

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

            // Init TeamService \\
            this.teamService = new TeamService(database);
            this.teamService.loadAll();

            // Init ChestService \\
            int maxChestsPerTeamMember = getConfig().getInt("chest.max_per_team_member", 1);
            this.chestService = new ChestService(database, maxChestsPerTeamMember);
            this.chestService.loadAll();

            // Init PlayerNameService \\
            this.playerNameService = new PlayerNameService();
        } catch (Exception e) {
            getLogger().severe("Failed to initialize database/repository: " + e.getMessage());
            // Disable plugin if critical
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Init Translations \\
        I18nManager.init();

        // Register Commands \\
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralCommandNode<CommandSourceStack> teamCommand = new TeamCommand().getCommand();
            commands.registrar().register(teamCommand);
        });

        // Player List Update Scheduler \\
        Bukkit.getScheduler().runTaskTimer(this, PlayerListComponents::updateGlobalPlayerlist, 1L, 100L);

        // Events \\
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), this);

        getLogger().info("Plugin initialized.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled.");
    }

}
