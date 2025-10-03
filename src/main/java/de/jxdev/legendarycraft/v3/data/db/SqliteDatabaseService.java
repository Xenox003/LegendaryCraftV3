package de.jxdev.legendarycraft.v3.data.db;

import de.jxdev.legendarycraft.v3.data.db.util.RowMapper;
import de.jxdev.legendarycraft.v3.data.db.util.SQLFunction;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class SqliteDatabaseService implements IDatabaseService {
    private final String url;

    public SqliteDatabaseService(Path file) {
        // e.g. plugin.getDataFolder().resolve("database.db")
        this.url = "jdbc:sqlite:" + file.toAbsolutePath();
    }

    /**
     * Get a connection to the database.
     * @return
     * @throws SQLException
     */
    private Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys=ON");
        }
        return c;
    }

    /**
     * Prepare a statement with or without Generated Keys.
     */
    private PreparedStatement prepare(Connection c, String sql, boolean returnKeys, Object... params) throws SQLException {
        PreparedStatement ps = returnKeys
                ? c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                : c.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
        return ps;
    }

    @Override
    public void init() throws SQLException {
        try (Connection c = getConnection();
             Statement st = c.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
            st.execute("PRAGMA foreign_keys=ON");
            st.execute("PRAGMA busy_timeout=5000");

            // Table Teams \\
            st.execute("""
                CREATE TABLE IF NOT EXISTS teams(
                    id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    name        TEXT NOT NULL UNIQUE,
                    prefix      TEXT NOT NULL,
                    color       INT NOT NULL,
                    created     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            // Trigger for teams update \\
            st.execute("""
                CREATE TRIGGER IF NOT EXISTS trg_teams_updated
                AFTER UPDATE ON teams
                  FOR EACH ROW BEGIN
                      UPDATE teams SET updated = CURRENT_TIMESTAMP WHERE id = NEW.id;
                  END
            """);

            // Table Team Members \\
            st.execute("""
                CREATE TABLE IF NOT EXISTS team_members(
                    player_id   TEXT PRIMARY KEY NOT NULL,
                    team_id     INTEGER NOT NULL,
                    role        TEXT NOT NULL DEFAULT 'MEMBER',
                    memberSince TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE
                )
            """);

            // Table for Team Invites \\
            st.execute("""
                CREATE TABLE IF NOT EXISTS team_invites(
                    player_id   TEXT NOT NULL,
                    team_id     INTEGER NOT NULL,
                    expires     TIMESTAMP NOT NULL DEFAULT (datetime('now', '+1 day')),
                    created     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (player_id, team_id),
                    FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE
                )
            """);

            // Table for Locked Chests \\
            st.execute("""
                CREATE TABLE IF NOT EXISTS locked_chests(
                    team_id     INTEGER NOT NULL,
                    world_id    TEXT NOT NULL,
                    x           INTEGER NOT NULL,
                    y           INTEGER NOT NULL,
                    z           INTEGER NOT NULL,
                    created     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (team_id, world_id, x, y, z),
                    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
                )
            """);

            // Table for Discord Link Codes \\
            st.execute("""
                CREATE TABLE IF NOT EXISTS discord_link_codes(
                    code        TEXT PRIMARY KEY NOT NULL,
                    player_id   TEXT NOT NULL,
                    expires     TIMESTAMP NOT NULL DEFAULT (datetime('now', '+10 minutes')),
                    created     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Table for Discord-to-Minecraft Links \\
            st.execute("""
                CREATE TABLE IF NOT EXISTS discord_users(
                    discord_id  TEXT PRIMARY KEY NOT NULL,
                    player_id   TEXT NOT NULL
                )
            """);

            // Table for Discord Team Roles Mapping \\
            st.execute("""
                CREATE TABLE IF NOT EXISTS discord_team_roles(
                    team_id     INTEGER PRIMARY KEY,
                    role_id     TEXT NOT NULL,
                    FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE
                )
            """);
        }
    }

    @Override
    public void close() throws SQLException {

    }

    @Override
    public <T> T inTransaction(SQLFunction<Connection, T> work) throws SQLException {
        try (Connection c = getConnection()) {
            try {
                c.setAutoCommit(false);
                T result = work.apply(c);
                c.commit();
                return result;
            } catch (Exception e) {
                c.rollback();
                throw new SQLException(e);
            }
        }
    }

    @Override
    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (Connection c = getConnection();
             PreparedStatement ps = prepare(c, sql, false, params);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return Optional.of(mapper.map(rs));
            return Optional.empty();
        }
    }

    @Override
    public <T> List<T> queryList(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> out = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement ps = prepare(c, sql, false, params);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapper.map(rs));
        }
        return out;
    }

    @Override
    public <T> Set<T> querySet(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        Set<T> out = new HashSet<>();
        try (Connection c = getConnection();
             PreparedStatement ps = prepare(c, sql, false, params);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapper.map(rs));
        }
        return out;
    }


    @Override
    public int update(String sql, Object... params) throws SQLException {
        try (Connection c = getConnection();
             PreparedStatement ps = prepare(c, sql, false, params)) {
            return ps.executeUpdate();
        }
    }

    @Override
    public long insertAndReturnId(String sql, Object... params) throws SQLException {
        try (Connection c = getConnection();
             PreparedStatement ps = prepare(c, sql, true, params)) {
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            // Fallback for SQLite AUTOINCREMENT tables
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) return rs.getLong(1);
            }
            throw new SQLException("No generated key available.");
        }
    }

    @Override
    public int[] batch(String sql, List<Object[]> batchParams) throws SQLException {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            for (Object[] params : batchParams) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                ps.addBatch();
            }

            return ps.executeBatch(); // returns one int per row (row count)
        }
    }
}