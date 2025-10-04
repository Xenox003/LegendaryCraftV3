package de.jxdev.legendarycraft.v3.data.models;

import java.util.UUID;

public class PlayerStats {
    private final UUID playerId;
    private final long playtimeMs;
    private final int joinCount;

    public PlayerStats(UUID playerId, long playtimeMs, int joinCount) {
        this.playerId = playerId;
        this.playtimeMs = playtimeMs;
        this.joinCount = joinCount;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public long getPlaytimeMs() {
        return playtimeMs;
    }

    public int getJoinCount() {
        return joinCount;
    }
}