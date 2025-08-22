package de.jxdev.legendarycraft.v3.db;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Minimal SQLite database handler for Paper plugins.
 * <p>
 * Notes:
 * - Uses only java.sql types so it compiles without bundling a driver.
 * - At runtime, you still need the SQLite JDBC driver available (e.g., by shading org.xerial:sqlite-jdbc
 *   into your plugin or providing it on the server). This class will throw if no driver is present.
 */
public class SqliteDatabase {
    private final JavaPlugin plugin;
    private final File dbFile;
    private Connection connection;

    public SqliteDatabase(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), fileName);
    }

    /** Ensure the connection is established. Safe to call multiple times. */
    public synchronized void connect() throws SQLException {
        if (connection != null && !isClosed(connection)) {
            return;
        }
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for database file.");
        }
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);
        // Enable foreign key enforcement
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
    }

    public synchronized boolean isOpen() {
        return connection != null && !isClosed(connection);
    }

    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error closing SQLite connection", e);
            } finally {
                connection = null;
            }
        }
    }

    /** Execute INSERT/UPDATE/DELETE/DDL. Returns affected rows count (or 0 for DDL). */
    public synchronized int executeUpdate(String sql, Object... params) throws SQLException {
        ensureConnected();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindParams(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Query multiple rows using a mapper. */
    public synchronized <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        ensureConnected();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapper.mapRow(rs));
                }
                return out;
            }
        }
    }

    /** Query a single row or return null if none. */
    public synchronized <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? null : list.getFirst();
    }

    private void ensureConnected() throws SQLException {
        if (!isOpen()) {
            connect();
        }
    }

    private static boolean isClosed(Connection c) {
        try {
            return c.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }

    private static void bindParams(PreparedStatement ps, Object... params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int idx = i + 1;
            switch (p) {
                case null -> ps.setObject(idx, null);
                case byte[] bytes -> ps.setBytes(idx, bytes);
                case Instant instant -> ps.setTimestamp(idx, Timestamp.from(instant));
                case java.util.Date date -> ps.setTimestamp(idx, new Timestamp(date.getTime()));
                case UUID uuid -> ps.setString(idx, p.toString());
                default -> ps.setObject(idx, p);
            }
        }
    }

    public synchronized void initializeTeamSchema() throws SQLException {
        ensureConnected();
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS teams (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL UNIQUE, " +
                    "prefix TEXT, " +
                    "color TEXT)");

            st.execute("CREATE TABLE IF NOT EXISTS team_members (" +
                    "user TEXT NOT NULL, " +
                    "team INTEGER NOT NULL, " +
                    "permission TEXT NOT NULL CHECK (permission IN ('user','admin','owner')), " +
                    "PRIMARY KEY (user, team), " +
                    "FOREIGN KEY (team) REFERENCES teams(id) ON DELETE CASCADE ON UPDATE CASCADE)");
        }
    }

    public interface RowMapper<T> {
        T mapRow(ResultSet rs) throws SQLException;
    }
}
