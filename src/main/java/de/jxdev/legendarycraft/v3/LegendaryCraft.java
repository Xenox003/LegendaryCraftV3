package de.jxdev.legendarycraft.v3;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jxdev.legendarycraft.v3.commands.ChestCommand;
import de.jxdev.legendarycraft.v3.commands.TeamCommand;
import de.jxdev.legendarycraft.v3.data.cache.LockedChestCache;
import de.jxdev.legendarycraft.v3.data.cache.TeamCache;
import de.jxdev.legendarycraft.v3.data.db.IDatabaseService;
import de.jxdev.legendarycraft.v3.data.db.SqliteDatabaseService;
import de.jxdev.legendarycraft.v3.data.repository.LockedChestRepository;
import de.jxdev.legendarycraft.v3.data.repository.TeamRepository;
import de.jxdev.legendarycraft.v3.event.team.*;
import de.jxdev.legendarycraft.v3.listener.PlayerChatListener;
import de.jxdev.legendarycraft.v3.listener.PlayerJoinLeaveListener;
import de.jxdev.legendarycraft.v3.event.EventDispatcher;
import de.jxdev.legendarycraft.v3.event.listeners.TeamTagUpdater;
import de.jxdev.legendarycraft.v3.playerlist.PlayerListComponents;
import de.jxdev.legendarycraft.v3.service.*;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.sql.SQLException;

@Getter
public final class LegendaryCraft extends JavaPlugin {

    public LegendaryCraft() {
        LegendaryCraft.setInstance(this);
    }

    @Setter
    @Getter
    private static LegendaryCraft instance;

    private IDatabaseService database;

    private TeamRepository teamRepository;
    private LockedChestRepository lockedChestRepository;

    private TeamCache teamCache;
    private LockedChestCache lockedChestCache;

    private TeamService teamService;
    private ChestService chestService;
    private PlayerNameService playerNameService;

    private EventDispatcher eventDispatcher;

    @Override
    public void onEnable() {
        try {
            // Ensure config exists \
            saveDefaultConfig();

            // Initialize Database using data folder + config value
            String dbFileName = getConfig().getString("database.file", "storage.db");
            Path dbPath = getDataFolder().toPath().resolve(dbFileName);
            this.database = new SqliteDatabaseService(dbPath);
            this.database.init();

            // Init DB Repositories \\
            this.teamRepository = new TeamRepository(database);
            this.lockedChestRepository = new LockedChestRepository(database);

            // Init Caches \\
            this.teamCache = new TeamCache();
            this.teamRepository.findAll().forEach(team -> teamCache.indexTeam(team));

            this.lockedChestCache = new LockedChestCache();
            this.lockedChestRepository.findAll().forEach(chest -> lockedChestCache.index(chest));

            // Init Services \
            int maxChestsPerTeamMember = getConfig().getInt("chest.max_per_team_member", 1);

            // Init Event System \
            this.eventDispatcher = new EventDispatcher();
            TeamTagUpdater tagUpdater = new TeamTagUpdater();
            eventDispatcher.registerListener(PlayerAddedToTeamEvent.class, tagUpdater.onPlayerAdded());
            eventDispatcher.registerListener(PlayerRemovedFromTeamEvent.class, tagUpdater.onPlayerRemoved());
            eventDispatcher.registerListener(TeamColorChangedEvent.class, tagUpdater.onTeamColorChanged());
            eventDispatcher.registerListener(TeamPrefixChangedEvent.class, tagUpdater.onTeamPrefixChanged());
            eventDispatcher.registerListener(TeamDeletedEvent.class, tagUpdater.onTeamDeleted());
            eventDispatcher.registerListener(TeamCreatedEvent.class, tagUpdater.onTeamCreated());


            this.teamService = new TeamServiceImpl(teamRepository, teamCache, eventDispatcher);
            this.chestService = new ChestServiceImpl(lockedChestRepository, lockedChestCache, maxChestsPerTeamMember);

            this.playerNameService = new PlayerNameServiceImpl();

            // Init Translations \
            I18nManager.init();

            // Register Commands \\
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
                LiteralCommandNode<CommandSourceStack> teamCommand = new TeamCommand().getCommand();
                commands.registrar().register(teamCommand);

                LiteralCommandNode<CommandSourceStack> chestCommand = new ChestCommand().getCommand();
                commands.registrar().register(chestCommand);
            });

            // Player List Update Scheduler \\
            Bukkit.getScheduler().runTaskTimer(this, PlayerListComponents::updateGlobalPlayerlist, 1L, 100L);

            // Events \\
            Bukkit.getPluginManager().registerEvents(new PlayerJoinLeaveListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerChatListener(), this);

            getLogger().info("Plugin initialized.");
        } catch (Exception ex) {
            getLogger().severe("Failed to initialize plugin: " + ex);
            ex.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            this.database.close();
        } catch (SQLException ex) {
            getLogger().severe("failed to disconnect from DB: " + ex.getMessage());
        }

        getLogger().info("Plugin disabled.");
    }

}
