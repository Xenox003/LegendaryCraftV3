package de.jxdev.legendarycraft.v3.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory service to track whether a player has team chat toggled on.
 */
public class TeamChatService {
    private final ConcurrentHashMap<UUID, Boolean> teamChatEnabled = new ConcurrentHashMap<>();

    public boolean isEnabled(UUID playerId) {
        return teamChatEnabled.getOrDefault(playerId, false);
    }

    public boolean toggle(UUID playerId) {
        boolean newValue = !isEnabled(playerId);
        teamChatEnabled.put(playerId, newValue);
        return newValue;
    }

    public void set(UUID playerId, boolean enabled) {
        teamChatEnabled.put(playerId, enabled);
    }
}
