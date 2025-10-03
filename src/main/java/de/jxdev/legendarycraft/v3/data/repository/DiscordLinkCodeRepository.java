package de.jxdev.legendarycraft.v3.data.repository;

import de.jxdev.legendarycraft.v3.data.db.IDatabaseService;
import de.jxdev.legendarycraft.v3.data.models.discord.DiscordLinkCode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class DiscordLinkCodeRepository {
    private final IDatabaseService db;

    public DiscordLinkCodeRepository(IDatabaseService db) {
        this.db = db;
    }

    private DiscordLinkCode map(ResultSet rs) throws SQLException {
        DiscordLinkCode code = new DiscordLinkCode();
        code.setCode(rs.getString("code"));
        code.setPlayerId(UUID.fromString(rs.getString("player_id")));
        // SQLite returns text for datetime('now') unless configured; use getString and parse to Instant if possible
        String expires = rs.getString("expires");
        String created = rs.getString("created");
        code.setExpires(expires != null ? Instant.parse(expires.replace(' ', 'T') + "Z") : null);
        code.setCreated(created != null ? Instant.parse(created.replace(' ', 'T') + "Z") : null);
        return code;
    }

    public void create(String code, UUID playerId) throws SQLException {
        db.update("INSERT INTO discord_link_codes(code, player_id) VALUES(?, ?)", code, playerId.toString());
    }

    public Optional<DiscordLinkCode> findByCode(String code) throws SQLException {
        return db.queryOne("SELECT code, player_id, expires, created FROM discord_link_codes WHERE code=?", this::map, code);
    }

    public Optional<DiscordLinkCode> findValidByCode(String code) throws SQLException {
        return db.queryOne("SELECT code, player_id, expires, created FROM discord_link_codes WHERE code=? AND expires > datetime('now')", this::map, code);
    }

    public void delete(String code) throws SQLException {
        db.update("DELETE FROM discord_link_codes WHERE code=?", code);
    }

    public int deleteExpired() throws SQLException {
        return db.update("DELETE FROM discord_link_codes WHERE expires <= datetime('now')");
    }
}