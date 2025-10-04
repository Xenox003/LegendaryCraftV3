package de.jxdev.legendarycraft.v3.listener;

import de.jxdev.legendarycraft.v3.service.SpawnDebuffService;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

@RequiredArgsConstructor
public class CombatTagListener implements Listener {
    private final SpawnDebuffService spawnDebuffService;

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim) {
            spawnDebuffService.tagCombat(victim);
        }
        if (event.getDamager() instanceof Player damager) {
            spawnDebuffService.tagCombat(damager);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player victim) {
            // Environmental damage should also tag the player (to avoid abuse)
            spawnDebuffService.tagCombat(victim);
        }
    }
}
