package de.jxdev.legendarycraft.v3.invsee;

import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class InvSeeHolder implements InventoryHolder {
    @Getter
    private final UUID targetId;
    private final boolean isEditable;
    private final boolean isOffline;

    public InvSeeHolder(OfflinePlayer target, boolean isEditable) {
        this.targetId = target.getUniqueId();
        this.isEditable = isEditable;
        this.isOffline = target.isOnline();
    }

    @Override public Inventory getInventory() { return null; }

    public boolean isEditable() { return isEditable; }
    public boolean isOffline() { return isOffline; }
}
