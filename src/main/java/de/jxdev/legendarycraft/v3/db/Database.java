package de.jxdev.legendarycraft.v3.db;

import de.jxdev.legendarycraft.v3.entities.Team;

import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Database implements AutoCloseable {

    private final String url;

    public Database(Path dbFile) {
        this.url = "jdbc:sqlite:" + Objects.requireNonNull(dbFile).toAbsolutePath();
    }

    /* ---------- Init and Connect ---------- */

    public void init() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute("PRAGMA busy_timeout=3000");
            st.execute("PRAGMA foreign_keys=ON");
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");

            st.execute("""
                CREATE TABLE IF NOT EXISTS teams (
                  id         INTEGER PRIMARY KEY AUTOINCREMENT,
                  name       TEXT NOT NULL UNIQUE,
                  prefix     TEXT NOT NULL,
                  color      TEXT NOT NULL,
                  owned_by   TEXT NOT NULL,
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS team_members (
                  player  TEXT PRIMARY KEY,
                  team_id INTEGER NOT NULL,
                  FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE
                )
            """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_teams_name ON teams(name)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_members_team ON team_members(team_id)");

            // Trigger to auto-update updated_at
            st.execute("""
                CREATE TRIGGER IF NOT EXISTS trg_teams_updated_at
                AFTER UPDATE ON teams
                FOR EACH ROW BEGIN
                    UPDATE teams SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
                END;
            """);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    /* ---------- Team Operations ---------- */
    public Team createTeam(String name, String prefix, String color, UUID ownerUuid) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(
                "INSERT INTO teams (name, prefix, color, owned_by) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            st.setString(1, name);
            st.setString(2, prefix);
            st.setString(3, color);
            st.setString(4, ownerUuid.toString());
            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new Team(id, name, prefix, color, ownerUuid, new HashSet<>(), Instant.now(), Instant.now());
                }
            }
        }
        throw new SQLException("Failed to insert team (no key returned)");
    }

    public Optional<Team> findTeamByName(String name) throws SQLException {
        String sql = "SELECT id, name, prefix, color, owned_by, created_at, updated_at FROM teams WHERE LOWER(name)=LOWER(?)";
        try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, name);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapTeam(rs, loadMembers(c, rs.getInt("id"))));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Team> findTeamById(int id) throws SQLException {
        String sql = "SELECT id, name, prefix, color, owned_by, created_at, updated_at FROM teams WHERE id=?";
        try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapTeam(rs, loadMembers(c, id)));
                }
            }
        }
        return Optional.empty();
    }

    public List<Team> listTeams() throws SQLException {
        String sql = "SELECT id, name, prefix, color, owned_by, created_at, updated_at FROM teams";
        try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            try (ResultSet rs = st.executeQuery()) {
                List<Team> teams = new ArrayList<>();
                Map<Integer, Set<UUID>> membersByTeam = loadAllMembers(c);
                while (rs.next()) {
                    int id = rs.getInt("id");
                    Set<UUID> members = membersByTeam.getOrDefault(id, Collections.emptySet());
                    teams.add(mapTeam(rs, members));
                }
                return teams;
            }
        }
    }

    public void updateTeam(Team team) throws SQLException {
        String sql = "UPDATE teams SET name=?, prefix=?, color=?, owned_by=? WHERE id=?";
        try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, team.getName());
            st.setString(2, team.getPrefix());
            st.setString(3, team.getColor());
            st.setString(4, team.getOwnerUuid().toString());
            st.setInt(5, team.getId());
            st.executeUpdate();
        }
    }

    public void deleteTeam(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement("DELETE FROM teams WHERE id=?")) {
            st.setInt(1, id);
            st.executeUpdate();
        }
    }

    public void addMember(UUID playerUuid, int teamId) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(
                "INSERT OR REPLACE INTO team_members(player, team_id) VALUES(?, ?)")) {
            st.setString(1, playerUuid.toString());
            st.setInt(2, teamId);
            st.executeUpdate();
        }
    }

    public void removeMember(UUID playerUuid) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(
                "DELETE FROM team_members WHERE player=?")) {
            st.setString(1, playerUuid.toString());
            st.executeUpdate();
        }
    }

    public Optional<Integer> findTeamIdByMember(UUID playerUuid) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(
                "SELECT team_id FROM team_members WHERE player=?")) {
            st.setString(1, playerUuid.toString());
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getInt(1));
            }
        }
        return Optional.empty();
    }

    private Team mapTeam(ResultSet rs, Set<UUID> members) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String prefix = rs.getString("prefix");
        String color = rs.getString("color");
        UUID owner = UUID.fromString(rs.getString("owned_by"));
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        Instant updatedAt = updatedTs != null ? updatedTs.toInstant() : createdAt;
        return new Team(id, name, prefix, color, owner, new HashSet<>(members), createdAt, updatedAt);
    }

    private Set<UUID> loadMembers(Connection c, int teamId) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT player FROM team_members WHERE team_id=?")) {
            st.setInt(1, teamId);
            try (ResultSet rs = st.executeQuery()) {
                Set<UUID> members = new HashSet<>();
                while (rs.next()) members.add(UUID.fromString(rs.getString(1)));
                return members;
            }
        }
    }

    private Map<Integer, Set<UUID>> loadAllMembers(Connection c) throws SQLException {
        Map<Integer, Set<UUID>> map = new HashMap<>();
        try (PreparedStatement st = c.prepareStatement("SELECT team_id, player FROM team_members"); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                int teamId = rs.getInt(1);
                UUID player = UUID.fromString(rs.getString(2));
                map.computeIfAbsent(teamId, k -> new HashSet<>()).add(player);
            }
        }
        return map;
    }

    @Override
    public void close() {
        // nothing to close for SQLite URL connections; method kept for AutoCloseable compatibility
    }
}
