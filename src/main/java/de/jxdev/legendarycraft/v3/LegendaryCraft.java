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
import de.jxdev.legendarycraft.v3.discord.ReadyListener;
import de.jxdev.legendarycraft.v3.event.team.*;
import de.jxdev.legendarycraft.v3.listener.*;
import de.jxdev.legendarycraft.v3.event.EventDispatcher;
import de.jxdev.legendarycraft.v3.event.listeners.TeamTagUpdater;
import de.jxdev.legendarycraft.v3.playerlist.PlayerListComponents;
import de.jxdev.legendarycraft.v3.service.*;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

    private TeamCache teamCache;
    private LockedChestCache lockedChestCache;

    private TeamService teamService;
    private ChestService chestService;
    private PlayerNameService playerNameService;

    private EventDispatcher eventDispatcher;

    private JDA jda;

    public void updateDiscordPresence() {
        try {
            JDA j = this.jda;
            if (j == null) return;
            int online = Bukkit.getOnlinePlayers().size();
            String text = String.format("%d Spieler online", online);
            j.getPresence().setActivity(Activity.customStatus(text));
        } catch (Throwable t) {
            // Be defensive: never let presence updates crash anything
            getLogger().log(Level.FINE, "Failed to update Discord presence", t);
        }
    }

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


            this.teamService = new TeamService(teamRepository, teamCache, eventDispatcher);
            this.chestService = new ChestService(lockedChestRepository, lockedChestCache, maxChestsPerTeamMember);

            this.playerNameService = new PlayerNameService();

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

            // Discord Presence Update Scheduler (every 30s) \\
            Bukkit.getScheduler().runTaskTimer(this, this::updateDiscordPresence, 20L, 600L);

            // Events \\
            Bukkit.getPluginManager().registerEvents(new PlayerJoinLeaveListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerChatListener(), this);
            Bukkit.getPluginManager().registerEvents(new ColoredAnvilListener(), this);
            Bukkit.getPluginManager().registerEvents(new ColoredSignListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerHeadDropListener(), this);

            getLogger().info("Plugin initialized.");
        } catch (Exception ex) {
            getLogger().severe("Failed to initialize plugin: " + ex);
            ex.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }

        CompletableFuture.runAsync(() -> {
            try {
                EnumSet<GatewayIntent> intents = EnumSet.of(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS
                        // Add only what you need. MESSAGE_CONTENT is privileged; enable in bot settings if used.
                        // GatewayIntent.MESSAGE_CONTENT
                );

                JDA built = JDABuilder.createDefault(this.getConfig().getString("discord.access_token"), intents)
                        .disableCache(net.dv8tion.jda.api.utils.cache.CacheFlag.SCHEDULED_EVENTS,
                                net.dv8tion.jda.api.utils.cache.CacheFlag.EMOJI)
                        .addEventListeners(new ReadyListener())
                        .build(); // login is async

                this.jda = built;
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to start JDA", e);
                // Disable plugin on the main thread to be safe:
                Bukkit.getScheduler().runTask(this, () ->
                        getServer().getPluginManager().disablePlugin(this));
            }
        });
    }

    @Override
    public void onDisable() {
        try {
            this.database.close();
        } catch (SQLException ex) {
            getLogger().severe("failed to disconnect from DB: " + ex.getMessage());
        }

        JDA local = this.jda;
        if (local != null) {
            try {
                local.shutdown(); // graceful
                local.awaitShutdown(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            } finally {
                if (local.getStatus() != JDA.Status.SHUTDOWN) {
                    local.shutdownNow(); // force if still alive
                }
            }
        }

        getLogger().info("Plugin disabled.");
    }

}
