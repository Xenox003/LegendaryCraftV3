package de.jxdev.legendarycraft.v3.invsee.target;

import org.bukkit.inventory.Inventory;

public interface InvseeSession {
    public boolean canMove(int slot);

    /**
     * Called when the viewer UI changes
     */
    public void pushToTarget();

    /**
     * Called when the target Inventory changes
     */
    public void pullToGui();

    /**
     * Called when the viewer Inventory is closed
     */
    public void save();

    /**
     * Called when the viewer Inventory is opened
     */
    public void load();

    Inventory getGui();

    public String getTargetKey();
}
