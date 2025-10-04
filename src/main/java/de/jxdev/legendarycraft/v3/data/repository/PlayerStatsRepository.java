package de.jxdev.legendarycraft.v3.data.repository;

import de.jxdev.legendarycraft.v3.data.db.IDatabaseService;
import de.jxdev.legendarycraft.v3.data.models.PlayerStats;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class PlayerStatsRepository {
    private final IDatabaseService db;

    public PlayerStatsRepository(IDatabaseService db) {
        this.db = db;
    }

    private PlayerStats map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PlayerStats(
                UUID.fromString(rs.getString("player_id")),
                rs.getLong("playtime_ms"),
                rs.getInt("join_count")
        );
    }

    public void incrementJoinCount(UUID playerId) throws SQLException {
        db.update(
                "INSERT INTO player_stats(player_id, join_count, playtime_ms) VALUES(?, 1, 0) " +
                        "ON CONFLICT(player_id) DO UPDATE SET join_count = player_stats.join_count + 1",
                playerId.toString()
        );
    }

    public void addPlaytime(UUID playerId, long playtimeMillis) throws SQLException {
        if (playtimeMillis <= 0) return;
        db.update(
                "INSERT INTO player_stats(player_id, join_count, playtime_ms) VALUES(?, 0, ?) " +
                        "ON CONFLICT(player_id) DO UPDATE SET playtime_ms = player_stats.playtime_ms + excluded.playtime_ms",
                playerId.toString(), playtimeMillis
        );
    }

    public Optional<PlayerStats> findByPlayerId(UUID playerId) throws SQLException {
        return db.queryOne(
                "SELECT player_id, join_count, playtime_ms FROM player_stats WHERE player_id=?",
                this::map,
                playerId.toString()
        );
    }
}
