package de.jxdev.legendarycraft.v3.db;

import de.jxdev.legendarycraft.v3.models.Team;
import de.jxdev.legendarycraft.v3.models.TeamMemberRole;
import net.kyori.adventure.text.format.NamedTextColor;

import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public class Database implements AutoCloseable {

    private final String url;

    public Database(Path dbFile) {
        this.url = "jdbc:sqlite:" + Objects.requireNonNull(dbFile).toAbsolutePath();
    }

    /* ---------- Init and Connect ---------- */

    public void init() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            // I do not bother to build any migration logic here \\
            // Would make sense - yes - but i'm lazy and it's overkill for this simple project \\

            st.execute("PRAGMA busy_timeout=3000");
            st.execute("PRAGMA foreign_keys=ON");
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");

            // Table for Teams \\
            st.execute("""
                CREATE TABLE IF NOT EXISTS teams (
                  id         INTEGER PRIMARY KEY AUTOINCREMENT,
                  name       TEXT NOT NULL UNIQUE,
                  prefix     TEXT NOT NULL,
                  color      INT  NOT NULL,
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Table for Team Members \\
            st.execute("""
                CREATE TABLE IF NOT EXISTS team_members (
                  player    TEXT PRIMARY KEY,
                  team_id   INTEGER NOT NULL,
                  role      TEXT NOT NULL DEFAULT 'MEMBER',
                  FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE
                )
            """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_teams_name ON teams(name)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_members_team ON team_members(team_id)");

            // Table for Team Invitations \\
            st.execute("""
                CREATE TABLE IF NOT EXISTS team_invitations (
                  player    TEXT PRIMARY KEY,
                  team_id   INTEGER NOT NULL,
                  expires_at TIMESTAMP NOT NULL,
                  FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE
                )
            """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_invitations_team ON team_invitations(team_id)");

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
    public Team createTeam(String name, String prefix, NamedTextColor color, UUID ownerUuid) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement st = c.prepareStatement(
                    "INSERT INTO teams (name, prefix, color) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                st.setString(1, name);
                st.setString(2, prefix);
                st.setInt(3, color.value());
                st.executeUpdate();
                try (ResultSet rs = st.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        try (PreparedStatement memberSt = c.prepareStatement(
                                "INSERT INTO team_members (player, team_id, role) VALUES (?, ?, ?)")) {
                            memberSt.setString(1, ownerUuid.toString());
                            memberSt.setInt(2, id);
                            memberSt.setString(3, "OWNER");
                            memberSt.executeUpdate();
                        }
                        c.commit();
                        return new Team(id, name, prefix, color, Map.of(ownerUuid, TeamMemberRole.OWNER), Set.of(), Instant.now(), Instant.now());
                    }
                }
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
        throw new SQLException("Failed to insert team (no key returned)");
    }

    public void deleteTeam(int id) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement st = c.prepareStatement("DELETE FROM team_members WHERE team_id=?")) {
                    st.setInt(1, id);
                    st.executeUpdate();
                }
                try (PreparedStatement st = c.prepareStatement("DELETE FROM team_invitations WHERE team_id=?")) {
                    st.setInt(1, id);
                    st.executeUpdate();
                }
                try (PreparedStatement st = c.prepareStatement("DELETE FROM teams WHERE id=?")) {
                    st.setInt(1, id);
                    st.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public Optional<Team> findTeamByName(String name) throws SQLException {
        String sql = "SELECT id, name, prefix, color, created_at, updated_at FROM teams WHERE LOWER(name)=LOWER(?)";
        try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, name);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    int teamId = rs.getInt("id");
                    return Optional.of(mapTeam(rs, getTeamMembers(c, teamId), getTeamInvites(c, teamId)));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Team> findTeamById(int id) throws SQLException {
        String sql = "SELECT id, name, prefix, color, created_at, updated_at FROM teams WHERE id=?";
        try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    int teamId = rs.getInt("id");
                    return Optional.of(mapTeam(rs, getTeamMembers(c, teamId), getTeamInvites(c, teamId)));
                }
            }
        }
        return Optional.empty();
    }

    public List<Team> listTeams() throws SQLException {
        String sql = "SELECT id, name, prefix, color, created_at, updated_at FROM teams";
        try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            try (ResultSet rs = st.executeQuery()) {
                List<Team> teams = new ArrayList<>();
                Map<Integer, Map<UUID, TeamMemberRole>> membersByTeam = loadAllMembers(c);
                Map<Integer, Set<UUID>> invitesByTeam = loadAllInvites(c);
                while (rs.next()) {
                    int id = rs.getInt("id");
                    Map<UUID, TeamMemberRole> members = membersByTeam.getOrDefault(id, Collections.emptyMap());
                    Set<UUID> invites = invitesByTeam.getOrDefault(id, Collections.emptySet());
                    teams.add(mapTeam(rs, members, invites));
                }
                return teams;
            }
        }
    }

    public void updateTeam(Team team) throws SQLException {
        String sql = "UPDATE teams SET name=?, prefix=?, color=? WHERE id=?";
        try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, team.getName());
            st.setString(2, team.getPrefix());
            st.setInt(3, team.getColor().value());
            st.setInt(5, team.getId());
            st.executeUpdate();
        }
    }

    public void addTeamMember(int teamId, UUID playerUuid, TeamMemberRole role) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(
                "INSERT OR REPLACE INTO team_members(player, team_id, role) VALUES(?, ?, ?)")) {
            st.setString(1, playerUuid.toString());
            st.setInt(2, teamId);
            st.setString(3, role.name());
            st.executeUpdate();
        }
    }

    public void removeTeamMembers(UUID playerUuid) throws SQLException {
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

    private Team mapTeam(ResultSet rs, Map<UUID, TeamMemberRole> members, Set<UUID> invites) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String prefix = rs.getString("prefix");
        NamedTextColor color = NamedTextColor.namedColor(rs.getInt("color"));
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        Instant updatedAt = updatedTs != null ? updatedTs.toInstant() : createdAt;
        return new Team(id, name, prefix, color, new HashMap<>(members), new HashSet<>(invites), createdAt, updatedAt);
    }

    private Map<UUID, TeamMemberRole> getTeamMembers(Connection c, int teamId) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT player, role FROM team_members WHERE team_id=?")) {
            st.setInt(1, teamId);
            try (ResultSet rs = st.executeQuery()) {
                Map<UUID, TeamMemberRole> members = new HashMap<>();
                while (rs.next()) members.put(UUID.fromString(rs.getString(1)), TeamMemberRole.valueOf(rs.getString(2)));
                return members;
            }
        }
    }

    private Map<Integer, Map<UUID, TeamMemberRole>> loadAllMembers(Connection c) throws SQLException {
        Map<Integer, Map<UUID, TeamMemberRole>> map = new HashMap<>();
        try (PreparedStatement st = c.prepareStatement("SELECT team_id, player, role FROM team_members"); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                int teamId = rs.getInt(1);
                UUID player = UUID.fromString(rs.getString(2));
                TeamMemberRole role = TeamMemberRole.valueOf(rs.getString(3));
                map.computeIfAbsent(teamId, k -> new HashMap<>()).put(player, role);
            }
        }
        return map;
    }

    private Set<UUID> getTeamInvites(Connection c, int teamId) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT player FROM team_invitations WHERE team_id=?")) {
            st.setInt(1, teamId);
            try (ResultSet rs = st.executeQuery()) {
                Set<UUID> invites = new HashSet<>();
                while (rs.next()) invites.add(UUID.fromString(rs.getString(1)));
                return invites;
            }
        }
    }

    private Map<Integer, Set<UUID>> loadAllInvites(Connection c) throws SQLException {
        Map<Integer, Set<UUID>> map = new HashMap<>();
        try (PreparedStatement st = c.prepareStatement("SELECT team_id, player FROM team_invitations"); ResultSet rs = st.executeQuery()) {
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
