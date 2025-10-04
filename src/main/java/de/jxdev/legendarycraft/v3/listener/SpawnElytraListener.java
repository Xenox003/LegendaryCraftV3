package de.jxdev.legendarycraft.v3.listener;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class SpawnElytraListener extends BukkitRunnable implements Listener {


    private final List<Player> playersFlying = new ArrayList<>();

    private final List<Player> playersBoosted = new ArrayList<>();

    private final LegendaryCraft plugin;

    private final int boostValue;

    private final int elytraRadius;

    private final boolean boostEnabled;

    private final World world;

    private void disablePlayerFlight(Player player) {
        player.setGliding(false);
        this.playersBoosted.remove(player);
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.playersFlying.remove(player), 5L);
    }

    private void enablePlayerFlight(Player player) {
        player.setGliding(true);
        this.playersFlying.add(player);
    }

    private boolean assertPlayerGameMode(Player player) {
        if (player.getGameMode() != GameMode.SURVIVAL)
            return false;
        return true;
    }

    private boolean isInRadius(Player player) {
        if (!player.getWorld().equals(this.world))
            return false;
        return (this.world.getSpawnLocation().distance(player.getLocation()) <= this.elytraRadius);
    }

    public SpawnElytraListener(LegendaryCraft plugin, int boostValue, int elytraRadius, World world) {
        this.plugin = plugin;
        this.boostValue = boostValue;
        this.elytraRadius = elytraRadius;
        this.boostEnabled = true;
        this.world = world;
        runTaskTimer(this.plugin, 0L, 3L);
    }

    public void run() {
        this.world.getPlayers().forEach(player -> {
            if (!assertPlayerGameMode(player))
                return;
            player.setAllowFlight(isInRadius(player));
            if (this.playersFlying.contains(player) && !player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isAir()) {
                disablePlayerFlight(player);
                player.setAllowFlight(false);
            }
        });
    }

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        if (!assertPlayerGameMode(event.getPlayer()))
            return;
        if (!isInRadius(event.getPlayer()))
            return;
        event.setCancelled(true);
        enablePlayerFlight(event.getPlayer());
        if (this.boostEnabled)
            event.getPlayer().sendActionBar(Component.translatable("elytra.info.boost", Component.keybind("key.swapOffhand")));
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.PLAYER && (event
                .getCause() == EntityDamageEvent.DamageCause.FALL || event
                .getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL) && this.playersFlying
                .contains(event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onSwapItem(PlayerSwapHandItemsEvent event) {
        if (!this.boostEnabled ||
                !this.playersFlying.contains(event.getPlayer()) || this.playersBoosted
                .contains(event.getPlayer()))
            return;
        event.setCancelled(true);
        this.playersBoosted.add(event.getPlayer());
        event.getPlayer().setVelocity(event.getPlayer().getLocation().getDirection().multiply(this.boostValue));
    }

    @EventHandler
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (event.getEntityType() == EntityType.PLAYER && this.playersFlying.contains(event.getEntity()))
            event.setCancelled(true);
    }
}
