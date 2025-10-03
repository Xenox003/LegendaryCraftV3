package de.jxdev.legendarycraft.v3.service;

import de.jxdev.legendarycraft.v3.data.repository.DiscordLinkCodeRepository;
import de.jxdev.legendarycraft.v3.data.repository.DiscordUserRepository;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class LinkService {
    private final DiscordLinkCodeRepository linkCodeRepo;
    private final DiscordUserRepository userRepo;
    private final SecureRandom random = new SecureRandom();

    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    public LinkService(DiscordLinkCodeRepository linkCodeRepo, DiscordUserRepository userRepo) {
        this.linkCodeRepo = linkCodeRepo;
        this.userRepo = userRepo;
    }

    /**
     * Generates a new link code for a player and stores it in the DB with default expiry (10 minutes).
     */
    public String generateLinkCode(UUID playerId) throws SQLException {
        // Try a few times to avoid collisions
        for (int i = 0; i < 5; i++) {
            String code = randomCode(6);
            // optimistic insert; if conflict, retry
            try {
                linkCodeRepo.create(code, playerId);
                return code;
            } catch (SQLException ex) {
                // if unique constraint fails, retry; otherwise rethrow
                if (!isUniqueViolation(ex)) throw ex;
            }
        }
        throw new SQLException("Failed to generate unique link code after multiple attempts");
    }

    /**
     * Links a Discord user using a link code. Returns the linked playerId on success.
     */
    public Optional<UUID> linkDiscordWithCode(String discordId, String code) throws SQLException {
        // Only accept non-empty code
        if (code == null || code.isBlank()) return Optional.empty();
        String normalized = code.trim().toUpperCase(Locale.ROOT);

        // only accept non-expired code
        return linkCodeRepo.findValidByCode(normalized).map(linkCode -> {
            try {
                userRepo.link(discordId, linkCode.getPlayerId());
                // consume code
                linkCodeRepo.delete(normalized);
                return Optional.of(linkCode.getPlayerId());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }).orElse(Optional.empty());
    }

    public boolean unlinkByPlayer(UUID playerId) throws SQLException {
        var existing = userRepo.findByPlayerId(playerId);
        if (existing.isPresent()) {
            userRepo.unlinkByPlayerId(playerId);
            return true;
        }
        return false;
    }

    public Optional<UUID> getLinkedPlayerByDiscord(String discordId) throws SQLException {
        return userRepo.findByDiscordId(discordId).map(l -> l.getPlayerId());
    }

    private String randomCode(int len) {
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) buf[i] = CODE_CHARS[random.nextInt(CODE_CHARS.length)];
        return new String(buf);
    }

    private boolean isUniqueViolation(SQLException ex) {
        // SQLite uses error code 19 for constraint violation
        return ex.getErrorCode() == 19 || (ex.getMessage() != null && ex.getMessage().toLowerCase(Locale.ROOT).contains("constraint"));
    }
}
