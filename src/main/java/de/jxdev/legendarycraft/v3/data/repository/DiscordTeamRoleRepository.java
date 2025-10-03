package de.jxdev.legendarycraft.v3.data.repository;

import de.jxdev.legendarycraft.v3.data.db.IDatabaseService;

import java.sql.SQLException;
import java.util.Optional;

public class DiscordTeamRoleRepository {
    private final IDatabaseService db;

    public DiscordTeamRoleRepository(IDatabaseService db) {
        this.db = db;
    }

    public Optional<String> findRoleIdByTeamId(int teamId) throws SQLException {
        return db.queryOne("SELECT role_id FROM discord_team_roles WHERE team_id=?", rs -> rs.getString("role_id"), teamId);
    }

    public void upsert(int teamId, String roleId) throws SQLException {
        db.update("INSERT INTO discord_team_roles(team_id, role_id) VALUES(?, ?) ON CONFLICT(team_id) DO UPDATE SET role_id=excluded.role_id", teamId, roleId);
    }

    public void delete(int teamId) throws SQLException {
        db.update("DELETE FROM discord_team_roles WHERE team_id=?", teamId);
    }
}
