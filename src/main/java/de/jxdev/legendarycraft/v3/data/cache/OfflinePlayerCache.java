package de.jxdev.legendarycraft.v3.data.cache;

import de.jxdev.legendarycraft.v3.argument.OfflinePlayerArgument;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OfflinePlayerCache implements Listener {
    private final ConcurrentHashMap<String, UUID> byName = new ConcurrentHashMap<>();

    public OfflinePlayerCache() {
        this.preloadOnce();
    }

    public void preloadOnce() {
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            String name = op.getName();
            if (name == null) continue;
            byName.put(op.getName(), op.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        byName.put(event.getPlayer().getName(), event.getPlayer().getUniqueId());
    }

    public OfflinePlayer resolve(String name) {
        if (name == null) return null;
        UUID id = byName.get(name);
        if (id == null) return null;
        return Bukkit.getOfflinePlayer(id);
    }

    public HashSet<String> getNames() {
        return new HashSet<>(byName.keySet());
    }
}
