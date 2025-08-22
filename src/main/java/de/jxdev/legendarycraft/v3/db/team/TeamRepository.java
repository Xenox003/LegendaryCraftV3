package de.jxdev.legendarycraft.v3.db.team;

import de.jxdev.legendarycraft.v3.db.SqliteDatabase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Teams and TeamMembers backed by SqliteDatabase.
 * Provides simple CRUD helpers and common queries.
 */
public class TeamRepository {

    private final SqliteDatabase db;

    public TeamRepository(SqliteDatabase db) {
        this.db = db;
    }

    // --------------------------- Teams ---------------------------

    public Team createTeam(Team team) throws SQLException {
        if (team.getName() == null || team.getName().isEmpty()) {
            throw new IllegalArgumentException("Team name required");
        }
        db.executeUpdate("INSERT INTO teams(name, prefix, color) VALUES(?,?,?)",
                team.getName(), team.getPrefix(), team.getColor());
        Integer id = db.queryOne("SELECT last_insert_rowid()", rs -> rs.getInt(1));
        team.setId(id);
        return team;
    }

    public Team getTeamById(int id) throws SQLException {
        return db.queryOne("SELECT id, name, prefix, color FROM teams WHERE id = ?",
                this::mapTeam, id);
    }

    public Team getTeamByName(String name) throws SQLException {
        return db.queryOne("SELECT id, name, prefix, color FROM teams WHERE name = ?",
                this::mapTeam, name);
    }

    public List<Team> listTeams() throws SQLException {
        return db.query("SELECT id, name, prefix, color FROM teams ORDER BY name ASC",
                this::mapTeam);
    }

    public boolean updateTeam(Team team) throws SQLException {
        if (team.getId() == null) throw new IllegalArgumentException("Team id required for update");
        int rows = db.executeUpdate("UPDATE teams SET name = ?, prefix = ?, color = ? WHERE id = ?",
                team.getName(), team.getPrefix(), team.getColor(), team.getId());
        return rows > 0;
    }

    public boolean deleteTeam(int id) throws SQLException {
        int rows = db.executeUpdate("DELETE FROM teams WHERE id = ?", id);
        return rows > 0;
    }

    private Team mapTeam(ResultSet rs) throws SQLException {
        return new Team(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("prefix"),
                rs.getString("color")
        );
    }

    // ------------------------ Team Members -----------------------

    public void addOrUpdateMember(UUID user, int teamId, PermissionLevel permission) throws SQLException {
        db.executeUpdate("INSERT INTO team_members(user, team, permission) VALUES(?,?,?) " +
                        "ON CONFLICT(user, team) DO UPDATE SET permission = excluded.permission",
                user.toString(), teamId, permission.getDbValue());
    }

    public boolean removeMember(UUID user, int teamId) throws SQLException {
        int rows = db.executeUpdate("DELETE FROM team_members WHERE user = ? AND team = ?",
                user.toString(), teamId);
        return rows > 0;
    }

    public List<TeamMember> getMembersByTeam(int teamId) throws SQLException {
        return db.query("SELECT user, team, permission FROM team_members WHERE team = ?",
                rs -> new TeamMember(
                        UUID.fromString(rs.getString("user")),
                        rs.getInt("team"),
                        PermissionLevel.fromDb(rs.getString("permission"))
                ), teamId);
    }

    public List<Team> getTeamsOfUser(UUID user) throws SQLException {
        return db.query("SELECT t.id, t.name, t.prefix, t.color " +
                        "FROM teams t JOIN team_members m ON m.team = t.id WHERE m.user = ?",
                this::mapTeam, user.toString());
    }

    /**
     * Convenience: returns the team of user if they are in exactly one team, else null.
     */
    public Team getUserCurrentTeam(UUID user) throws SQLException {
        List<Team> teams = getTeamsOfUser(user);
        return teams.isEmpty() ? null : teams.getFirst();
    }

    public int getMemberCount(int teamId) throws SQLException {
        Integer cnt = db.queryOne("SELECT COUNT(*) FROM team_members WHERE team = ?", rs -> rs.getInt(1), teamId);
        return cnt == null ? 0 : cnt;
    }

    public PermissionLevel getUserPermissionInTeam(UUID user, int teamId) throws SQLException {
        String perm = db.queryOne("SELECT permission FROM team_members WHERE user = ? AND team = ?",
                rs -> rs.getString(1), user.toString(), teamId);
        return perm == null ? null : PermissionLevel.fromDb(perm);
    }

    // --------------------------- Invites -------------------------

    public void addInvite(UUID user, int teamId) throws SQLException {
        db.executeUpdate("INSERT OR IGNORE INTO team_invites(user, team) VALUES(?,?)",
                user.toString(), teamId);
    }

    public boolean hasInvite(UUID user, int teamId) throws SQLException {
        Integer one = db.queryOne("SELECT 1 FROM team_invites WHERE user = ? AND team = ?",
                rs -> rs.getInt(1), user.toString(), teamId);
        return one != null;
    }

    public boolean removeInvite(UUID user, int teamId) throws SQLException {
        int rows = db.executeUpdate("DELETE FROM team_invites WHERE user = ? AND team = ?",
                user.toString(), teamId);
        return rows > 0;
    }

    public List<Team> getInvitesForUser(UUID user) throws SQLException {
        return db.query("SELECT t.id, t.name, t.prefix, t.color FROM teams t " +
                        "JOIN team_invites i ON i.team = t.id WHERE i.user = ?",
                this::mapTeam, user.toString());
    }
}
