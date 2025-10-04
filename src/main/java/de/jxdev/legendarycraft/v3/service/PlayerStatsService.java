package de.jxdev.legendarycraft.v3.service;

import de.jxdev.legendarycraft.v3.data.models.PlayerStats;
import de.jxdev.legendarycraft.v3.data.repository.PlayerStatsRepository;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatsService {
    private final PlayerStatsRepository repo;
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();

    public PlayerStatsService(PlayerStatsRepository repo) {
        this.repo = repo;
    }

    public void onPlayerJoin(UUID playerId) {
        try {
            repo.incrementJoinCount(playerId);
        } catch (SQLException e) {
            // Log if you have access to plugin logger in future; swallow to avoid impacting gameplay
        }
        sessionStart.put(playerId, System.currentTimeMillis());
    }

    public void onPlayerQuit(UUID playerId) {
        Long start = sessionStart.remove(playerId);
        if (start != null) {
            long duration = Math.max(0, System.currentTimeMillis() - start);
            try {
                repo.addPlaytime(playerId, duration);
            } catch (SQLException e) {
                // swallow; optionally log
            }
        }
    }

    public PlayerStats getOrCreateZero(UUID playerId) {
        try {
            Optional<PlayerStats> opt = repo.findByPlayerId(playerId);
            return opt.orElse(new PlayerStats(playerId, 0L, 0));
        } catch (SQLException e) {
            return new PlayerStats(playerId, 0L, 0);
        }
    }

    public long getTotalPlaytimeMsIncludingActive(UUID playerId) {
        PlayerStats base = getOrCreateZero(playerId);
        long total = base.getPlaytimeMs();
        Long start = sessionStart.get(playerId);
        if (start != null) {
            total += Math.max(0, System.currentTimeMillis() - start);
        }
        return total;
    }

    public int getJoinCount(UUID playerId) {
        return getOrCreateZero(playerId).getJoinCount();
    }

    public static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400; seconds %= 86400;
        long hours = seconds / 3600; seconds %= 3600;
        long minutes = seconds / 60; seconds %= 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}