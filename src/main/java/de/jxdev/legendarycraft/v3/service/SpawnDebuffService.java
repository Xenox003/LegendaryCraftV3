package de.jxdev.legendarycraft.v3.service;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cooldowns and combat tags for the /spawn command
 */
public class SpawnDebuffService {
    private final long cooldownMillis;
    private final long combatTagMillis;

    private final Map<UUID, Long> lastSpawnUse = new ConcurrentHashMap<>();
    private final Map<UUID, Long> combatTaggedUntil = new ConcurrentHashMap<>();

    public SpawnDebuffService(long cooldownSeconds, long combatTagSeconds) {
        this.cooldownMillis = Math.max(0, cooldownSeconds) * 1000L;
        this.combatTagMillis = Math.max(0, combatTagSeconds) * 1000L;
    }

    public void recordSpawnUse(Player player) {
        lastSpawnUse.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean isOnCooldown(Player player) {
        if (cooldownMillis <= 0) return false;
        Long last = lastSpawnUse.get(player.getUniqueId());
        if (last == null) return false;
        return System.currentTimeMillis() - last < cooldownMillis;
    }

    public long getCooldownRemainingSeconds(Player player) {
        if (cooldownMillis <= 0) return 0;
        Long last = lastSpawnUse.get(player.getUniqueId());
        if (last == null) return 0;
        long elapsed = System.currentTimeMillis() - last;
        long remaining = cooldownMillis - elapsed;
        return Math.max(0, (long) Math.ceil(remaining / 1000.0));
    }

    public void tagCombat(Player player) {
        if (combatTagMillis <= 0) return;
        combatTaggedUntil.put(player.getUniqueId(), System.currentTimeMillis() + combatTagMillis);
    }

    public boolean isInCombat(Player player) {
        if (combatTagMillis <= 0) return false;
        Long until = combatTaggedUntil.get(player.getUniqueId());
        return until != null && until > System.currentTimeMillis();
    }

    public long getCombatRemainingSeconds(Player player) {
        Long until = combatTaggedUntil.get(player.getUniqueId());
        if (until == null) return 0;
        long remaining = until - System.currentTimeMillis();
        return Math.max(0, (long) Math.ceil(remaining / 1000.0));
    }
}
