package de.jxdev.legendarycraft.v3.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class PlayerHeadDropListener implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && victim != null) {
            // Create player head item
            ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(victim); // set the skin to the victim's head
                meta.displayName(Component.text(victim.getName() + "' Kopf").color(NamedTextColor.GOLD));
                head.setItemMeta(meta);
            }

            // Drop the head at victim's location
            victim.getWorld().dropItemNaturally(victim.getLocation(), head);
        }
    }
}
