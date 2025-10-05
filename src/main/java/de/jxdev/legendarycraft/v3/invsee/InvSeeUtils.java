package de.jxdev.legendarycraft.v3.invsee;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class InvSeeUtils {
    public static ItemStack createBlockerItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ").color(NamedTextColor.DARK_GRAY));
        item.setItemMeta(meta);

        return item;
    }

    public static ItemStack cloneOrNull(ItemStack it) {
        return (it == null || it.getType().isAir()) ? null : it.clone();
    }
    public static ItemStack cloneOrAir(ItemStack it) {
        return (it == null || it.getType().isAir()) ? new ItemStack(Material.AIR) : it.clone();
    }
    public static Inventory createInventory(int size, Component title) {
        return Bukkit.createInventory(new InvSeeHolder(), size, title);
    }
    public static String getPlayerTargetKey(UUID playerId) {
        return "player-" + playerId.toString();
    }
}
