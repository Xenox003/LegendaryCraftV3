package de.jxdev.legendarycraft.v3.util;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.InventoryHolder;

import java.util.Collections;
import java.util.List;

public class ChestUtil {
    public static List<Chest> resolveChestGroup(Block chestBlock) {
        if (chestBlock == null || chestBlock.getType() != Material.CHEST) {
            return Collections.emptyList();
        }

        org.bukkit.block.Chest state = (org.bukkit.block.Chest) chestBlock.getState();
        InventoryHolder holder = state.getInventory().getHolder();

        if (holder instanceof DoubleChest dc) {
            // Double chest: return left + right as Chest block states
            InventoryHolder left = dc.getLeftSide();
            InventoryHolder right = dc.getRightSide();
            org.bukkit.block.Chest leftChest = (org.bukkit.block.Chest) ((BlockState) left).getBlock().getState();
            org.bukkit.block.Chest rightChest = (org.bukkit.block.Chest) ((BlockState) right).getBlock().getState();
            return List.of(leftChest, rightChest);
        }

        // Single chest
        return List.of(state);
    }
}
