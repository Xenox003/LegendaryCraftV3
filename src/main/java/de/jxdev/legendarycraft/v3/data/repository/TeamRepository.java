package de.jxdev.legendarycraft.v3.data.repository;

import de.jxdev.legendarycraft.v3.data.db.IDatabaseService;
import de.jxdev.legendarycraft.v3.data.models.team.Team;
import de.jxdev.legendarycraft.v3.data.models.team.TeamMember;
import de.jxdev.legendarycraft.v3.data.models.team.TeamMemberRole;
import de.jxdev.legendarycraft.v3.data.models.team.TeamWithMemberCount;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TeamRepository {
    private final IDatabaseService db;
    public TeamRepository(IDatabaseService db) {
        this.db = db;
    }

    private Team mapTeam(ResultSet rs) throws SQLException {
        Team team = new Team();
        team.setId(rs.getInt("id"));
        team.setName(rs.getString("name"));
        team.setPrefix(rs.getString("prefix"));
        team.setColor(NamedTextColor.namedColor(rs.getInt("color")));
        return team;
    }
    private TeamMember mapTeamMember(ResultSet rs) throws SQLException {
        TeamMember member = new TeamMember();
        member.setTeamId(rs.getInt("team_id"));
        member.setPlayerId(UUID.fromString(rs.getString("player_id")));
        member.setRole(TeamMemberRole.valueOf(rs.getString("role")));
        return member;
    }

    public List<Team> findAll() throws SQLException {
        return db.queryList("SELECT id,name,prefix,color FROM teams ORDER BY name", this::mapTeam);
    }

    public List<TeamWithMemberCount> findAllWithMemberCount() throws SQLException {
        return db.queryList("SELECT t.id, t.name, t.prefix, t.color, COUNT(tm.player_id) as member_count FROM teams t LEFT JOIN team_members tm ON t.id = tm.team_id GROUP BY t.id",rs ->

        {
            TeamWithMemberCount team = new TeamWithMemberCount();
            team.setId(rs.getInt("id"));
            team.setName(rs.getString("name"));
            team.setPrefix(rs.getString("prefix"));
            team.setColor(NamedTextColor.namedColor(rs.getInt("color")));
            team.setMemberCount(rs.getInt("member_count"));
            return team;
        });
    }

    public Optional<Team> findById(int id) throws SQLException {
        return db.queryOne("SELECT id,name,prefix,color FROM teams WHERE id=?", this::mapTeam, id);
    }

    public Optional<Team> findByName(String name) throws SQLException {
        return db.queryOne("SELECT id,name,prefix,color FROM teams WHERE name=?", this::mapTeam, name);
    }

    public List<TeamMember> findMembersByTeam(int teamId) throws SQLException {
        return db.queryList("SELECT player_id, team_id, role FROM team_members WHERE team_id=?", this::mapTeamMember, teamId);
    }

    public Optional<Team> findByPlayer(UUID playerId) throws SQLException {
        return db.queryOne("SELECT t.id,t.name,t.prefix,t.color FROM teams t INNER JOIN team_members tm ON t.id=tm.team_id WHERE tm.player_id=?", this::mapTeam, playerId.toString());
    }

    public int createTeam(String name, String prefix, NamedTextColor color, UUID creator) throws SQLException {
        return db.inTransaction(conn -> {
            // Insert team \\
            PreparedStatement statement = conn.prepareStatement("INSERT INTO teams(name,prefix,color) VALUES(?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setString(1, name);
            statement.setString(2, prefix);
            statement.setInt(3, color.value());
            statement.execute();
            int teamId = statement.getGeneratedKeys().getInt(1);

            // Insert creator as OWNER \\
            PreparedStatement teamMemberStatement = conn.prepareStatement("INSERT INTO team_members(team_id,player_id,role) VALUES(?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
            teamMemberStatement.setInt(1, teamId);
            teamMemberStatement.setString(2, creator.toString());
            teamMemberStatement.setString(3, TeamMemberRole.OWNER.name());
            teamMemberStatement.execute();

            return teamId;
        });
    }
    public void deleteTeam(int teamId) throws SQLException {
        db.update("DELETE FROM teams WHERE id=?", teamId);
    }

    public void updatePrefix(int teamId, String prefix) throws SQLException {
        db.update("UPDATE teams SET prefix=? WHERE id=?", prefix, teamId);
    }
    public void updateColor(int teamId, NamedTextColor color) throws SQLException {
        db.update("UPDATE teams SET color=? WHERE id=?", color.value(), teamId);
    }
    public void updateName(int teamId, String teamName) throws SQLException {
        db.update("UPDATE teams SET name=? WHERE id=?", teamName, teamId);
    }

    public void addMember(int teamId, UUID playerId, TeamMemberRole role) throws SQLException {
        db.update("INSERT INTO team_members(team_id,player_id,role) VALUES(?,?,?)", teamId, playerId.toString(), role.name());
    }
    public void removeMember(int teamId, UUID playerId) throws SQLException {
        db.update("DELETE FROM team_members WHERE team_id=? AND player_id=?", teamId, playerId.toString());
    }
    public void updateMemberRole(int teamId, UUID playerId, TeamMemberRole role) throws SQLException {
        db.update("UPDATE team_members SET role=? WHERE team_id=? AND player_id=?", role.name(), teamId, playerId.toString());
    }
    public Optional<TeamMemberRole> getMemberRole(int teamId, UUID playerId) throws SQLException {
        return db.queryOne("SELECT role FROM team_members WHERE team_id=? AND player_id=?", rs -> TeamMemberRole.valueOf(rs.getString("role")), teamId, playerId.toString());
    }
    public List<TeamMember> getMemberList(int teamId) throws SQLException {
        return db.queryList("SELECT player_id, team_id, role FROM team_members WHERE team_id=?", this::mapTeamMember, teamId);
    }
    public int getMemberCount(int teamId) throws SQLException {
        return db.queryOne("SELECT COUNT(*) as count FROM team_members WHERE team_id=?", rs -> rs.getInt("count"), teamId).orElse(0);
    }

    public void invitePlayerToTeam(int teamId, UUID playerId) throws SQLException {
        db.update("INSERT INTO team_invites(team_id,player_id) VALUES(?,?)", teamId, playerId.toString());
    }
    public void removeInviteFromTeam(int teamId, UUID playerId) throws SQLException {
        db.update("DELETE FROM team_invites WHERE team_id=? AND player_id=?", teamId, playerId.toString());
    }
    public void acceptTeamInvite(int teamId, UUID playerId) throws SQLException {
        db.inTransaction(conn -> {
            PreparedStatement invitationStatement = conn.prepareStatement("DELETE FROM team_invites WHERE team_id=? AND player_id=?");
            invitationStatement.setInt(1, teamId);
            invitationStatement.setString(2, playerId.toString());
            invitationStatement.executeUpdate();

            PreparedStatement statement = conn.prepareStatement("INSERT INTO team_members(team_id,player_id,role) VALUES(?,?,?)");
            statement.setInt(1, teamId);
            statement.setString(2, playerId.toString());
            statement.setString(3, TeamMemberRole.MEMBER.name());
            statement.executeUpdate();

            return null;
        });
    }
    public boolean isPlayerInvitedToTeam(UUID playerId, int teamId) throws SQLException {
        return db.queryOne("SELECT COUNT(*) as count FROM team_invites WHERE team_id=? AND player_id=? AND expires > datetime('now')", rs -> rs.getInt("count") > 0, teamId, playerId.toString()).orElse(false);
    }
}
