package de.jxdev.legendarycraft.v3.models;

import java.util.UUID;

/**
 * Represents a block position as a Record.
 * @param worldId
 * @param x
 * @param y
 * @param z
 */
public record BlockPos(UUID worldId, int x, int y, int z) {
    public static BlockPos fromBlock(org.bukkit.block.Block b) {
        return new BlockPos(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ());
    }
    public static BlockPos fromLocation(org.bukkit.Location loc) {
        return new BlockPos(
                loc.getWorld().getUID(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()
        );
    }
}