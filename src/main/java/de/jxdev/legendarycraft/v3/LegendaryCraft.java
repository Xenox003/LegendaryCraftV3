package de.jxdev.legendarycraft.v3;

import de.jxdev.legendarycraft.v3.db.SqliteDatabase;
import de.jxdev.legendarycraft.v3.i18n.Messages;
import de.jxdev.legendarycraft.v3.db.team.TeamRepository;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class LegendaryCraft extends JavaPlugin {

    private Messages messages;
    private SqliteDatabase database;
    private TeamRepository teamRepository;

    @Override
    public void onEnable() {
        // Ensure config exists
        saveDefaultConfig();

        String locale = getConfig().getString("locale", "de_DE");
        String fallback = getConfig().getString("fallback-locale", "de_DE");

        // Initialize i18n
        this.messages = new Messages(this, locale, fallback);

        // Initialize SQLite database
        String dbFile = getConfig().getString("database.file", "storage.db");
        this.database = new SqliteDatabase(this, dbFile);
        try {
            this.database.connect();
            this.database.initializeTeamSchema();
            getLogger().info("SQLite database connected: " + dbFile);
            getLogger().info("Team schema ensured (teams, team_members).");

            // Initialize repositories
            this.teamRepository = new TeamRepository(this.database);
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize SQLite database: " + e.getMessage());
        }

        // Example startup log using i18n
        getLogger().info("Plugin initialized.");
    }

    @Override
    public void onDisable() {
        if (messages != null) {
            getLogger().info("Plugin disabled.");
        }
        if (database != null && database.isOpen()) {
            database.close();
        }
    }

    public Messages getMessages() {
        return messages;
    }

    public SqliteDatabase getDatabase() {
        return database;
    }

    public TeamRepository getTeamRepository() {
        return teamRepository;
    }
}
