package de.jxdev.legendarycraft.v3.data.repository;

import de.jxdev.legendarycraft.v3.data.db.IDatabaseService;
import de.jxdev.legendarycraft.v3.data.models.discord.DiscordUserLink;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class DiscordUserRepository {
    private final IDatabaseService db;

    public DiscordUserRepository(IDatabaseService db) {
        this.db = db;
    }

    private DiscordUserLink map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new DiscordUserLink(rs.getString("discord_id"), UUID.fromString(rs.getString("player_id")));
    }

    public void link(String discordId, UUID playerId) throws SQLException {
        // Upsert by discord_id
        db.update("INSERT INTO discord_users(discord_id, player_id) VALUES(?, ?) ON CONFLICT(discord_id) DO UPDATE SET player_id=excluded.player_id", discordId, playerId.toString());
    }

    public Optional<DiscordUserLink> findByDiscordId(String discordId) throws SQLException {
        return db.queryOne("SELECT discord_id, player_id FROM discord_users WHERE discord_id=?", this::map, discordId);
    }

    public Optional<DiscordUserLink> findByPlayerId(UUID playerId) throws SQLException {
        return db.queryOne("SELECT discord_id, player_id FROM discord_users WHERE player_id=?", this::map, playerId.toString());
    }

    public void unlinkByDiscordId(String discordId) throws SQLException {
        db.update("DELETE FROM discord_users WHERE discord_id=?", discordId);
    }

    public void unlinkByPlayerId(UUID playerId) throws SQLException {
        db.update("DELETE FROM discord_users WHERE player_id=?", playerId.toString());
    }
}