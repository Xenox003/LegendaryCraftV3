package de.jxdev.legendarycraft.v3;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jxdev.legendarycraft.v3.commands.*;
import de.jxdev.legendarycraft.v3.data.cache.LockedChestCache;
import de.jxdev.legendarycraft.v3.data.cache.OfflinePlayerCache;
import de.jxdev.legendarycraft.v3.data.cache.TeamCache;
import de.jxdev.legendarycraft.v3.data.db.IDatabaseService;
import de.jxdev.legendarycraft.v3.data.db.SqliteDatabaseService;
import de.jxdev.legendarycraft.v3.data.repository.LockedChestRepository;
import de.jxdev.legendarycraft.v3.data.repository.TeamRepository;
import de.jxdev.legendarycraft.v3.data.repository.DiscordLinkCodeRepository;
import de.jxdev.legendarycraft.v3.data.repository.DiscordUserRepository;
import de.jxdev.legendarycraft.v3.data.repository.DiscordTeamRoleRepository;
import de.jxdev.legendarycraft.v3.data.repository.PlayerStatsRepository;
import de.jxdev.legendarycraft.v3.event.listeners.ChestTeamDeleter;
import de.jxdev.legendarycraft.v3.event.team.*;
import de.jxdev.legendarycraft.v3.invsee.InvSeeController;
import de.jxdev.legendarycraft.v3.listener.*;
import de.jxdev.legendarycraft.v3.event.EventDispatcher;
import de.jxdev.legendarycraft.v3.event.listeners.TeamTagUpdater;
import de.jxdev.legendarycraft.v3.playerlist.PlayerListComponents;
import de.jxdev.legendarycraft.v3.service.*;
import de.jxdev.legendarycraft.v3.listener.CombatTagListener;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Level;

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
    private DiscordLinkCodeRepository discordLinkCodeRepository;
    private DiscordUserRepository discordUserRepository;
    private DiscordTeamRoleRepository discordTeamRoleRepository;
    private PlayerStatsRepository playerStatsRepository;

    private TeamCache teamCache;
    private LockedChestCache lockedChestCache;
    private OfflinePlayerCache offlinePlayerCache;

    private TeamService teamService;
    private ChestService chestService;
    private PlayerNameService playerNameService;
    //private DiscordService discordService;
    //private DiscordTeamRoleSyncService discordTeamRoleSyncService;
    private LinkService linkService;
    private PlayerStatsService playerStatsService;
    private TeamChatService teamChatService;
    private SpawnDebuffService spawnDebuffService;

    private EventDispatcher eventDispatcher;

    private InvSeeController invSeeController;

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
            this.discordLinkCodeRepository = new DiscordLinkCodeRepository(database);
            this.discordUserRepository = new DiscordUserRepository(database);
            this.discordTeamRoleRepository = new DiscordTeamRoleRepository(database);
            this.playerStatsRepository = new PlayerStatsRepository(database);

            // Init Caches \\
            this.teamCache = new TeamCache();
            this.teamRepository.findAll().forEach(team -> teamCache.indexTeam(team));

            this.lockedChestCache = new LockedChestCache();
            this.lockedChestRepository.findAll().forEach(chest -> lockedChestCache.index(chest));

            this.offlinePlayerCache = new OfflinePlayerCache();

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

            ChestTeamDeleter chestTeamDeleter = new ChestTeamDeleter();
            eventDispatcher.registerListener(TeamDeletedEvent.class, chestTeamDeleter.onTeamDelete());



            this.teamService = new TeamService(teamRepository, teamCache, eventDispatcher);
            this.chestService = new ChestService(lockedChestRepository, lockedChestCache, maxChestsPerTeamMember);
            this.linkService = new LinkService(discordLinkCodeRepository, discordUserRepository);
            //this.discordService = new DiscordService(this);
            //this.discordTeamRoleSyncService = new DiscordTeamRoleSyncService(this, discordService, teamService, teamRepository, discordTeamRoleRepository, discordUserRepository);
            this.playerStatsService = new PlayerStatsService(playerStatsRepository);
            this.teamChatService = new TeamChatService();

            // Init Spawn Debuffs (cooldown + combat tag)
            long spawnCooldown = getConfig().getLong("spawn.cooldown_seconds", 10);
            long combatTag = getConfig().getLong("spawn.combat_tag_seconds", 10);
            this.spawnDebuffService = new SpawnDebuffService(spawnCooldown, combatTag);

            // Discord role sync listeners (after services are initialized)
            /*
            eventDispatcher.registerListener(TeamCreatedEvent.class, discordTeamRoleSyncService.onTeamCreated());
            eventDispatcher.registerListener(TeamDeletedEvent.class, discordTeamRoleSyncService.onTeamDeleted());
            eventDispatcher.registerListener(TeamColorChangedEvent.class, discordTeamRoleSyncService.onTeamColorChanged());
            eventDispatcher.registerListener(TeamNameChangedEvent.class, discordTeamRoleSyncService.onTeamNameChanged());
            eventDispatcher.registerListener(PlayerAddedToTeamEvent.class, discordTeamRoleSyncService.onPlayerAdded());
            eventDispatcher.registerListener(PlayerRemovedFromTeamEvent.class, discordTeamRoleSyncService.onPlayerRemoved());
             */

            this.playerNameService = new PlayerNameService();

            // Init Translations \
            I18nManager.init();

            // Register Commands \\
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
                LiteralCommandNode<CommandSourceStack> teamCommand = new TeamCommand().getCommand();
                commands.registrar().register(teamCommand);

                LiteralCommandNode<CommandSourceStack> chestCommand = new ChestCommand().getCommand();
                commands.registrar().register(chestCommand);

                LiteralCommandNode<CommandSourceStack> linkCommand = new de.jxdev.legendarycraft.v3.commands.LinkCommand().getCommand();
                commands.registrar().register(linkCommand);

                LiteralCommandNode<CommandSourceStack> statsCommand = new StatsCommand().getCommand();
                commands.registrar().register(statsCommand);

                LiteralCommandNode<CommandSourceStack> spawnCommand = new SpawnCommand().getCommand();
                commands.registrar().register(spawnCommand);

                LiteralCommandNode<CommandSourceStack> invseeCommand = new InvSeeCommand().getCommand();
                commands.registrar().register(invseeCommand);
            });

            // Player List Update Scheduler \\
            Bukkit.getScheduler().runTaskTimer(this, PlayerListComponents::updateGlobalPlayerlist, 1L, 100L);

            // Events \\
            Bukkit.getPluginManager().registerEvents(new PlayerJoinLeaveListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerChatListener(this.teamChatService, this.teamService), this);
            Bukkit.getPluginManager().registerEvents(new ColoredAnvilListener(), this);
            Bukkit.getPluginManager().registerEvents(new ColoredSignListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerHeadDropListener(), this);
            Bukkit.getPluginManager().registerEvents(new ChestProtectListener(this.chestService, this.teamService), this);
            Bukkit.getPluginManager().registerEvents(new CombatTagListener(this.spawnDebuffService), this);
            Bukkit.getPluginManager().registerEvents(new SpawnElytraListener(
                    this,
                    getConfig().getInt("elytra.boost_value", 10),
                    getConfig().getInt("elytra.radius", 25),
                    Bukkit.getWorld("world")
            ), this);
            Bukkit.getPluginManager().registerEvents(offlinePlayerCache, this);

            File worldFolder = Bukkit.getWorlds().getFirst().getWorldFolder();
            this.invSeeController = new InvSeeController(this, worldFolder);
            Bukkit.getPluginManager().registerEvents(invSeeController, this);

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

        try {
            //this.discordService.disable();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Failed to disable Discord integration: " + ex.getMessage(), ex);
        }

        getLogger().info("Plugin disabled.");
    }

}
