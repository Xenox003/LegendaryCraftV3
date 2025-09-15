package de.jxdev.legendarycraft.v3.service;

import de.jxdev.legendarycraft.v3.db.Database;
import de.jxdev.legendarycraft.v3.models.BlockPos;
import de.jxdev.legendarycraft.v3.models.LockedChest;
import de.jxdev.legendarycraft.v3.models.Team;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChestService {
    private final Database db;
    private final int maxChestsPerTeamMember;
    private final Map<Integer, Set<LockedChest>> byTeamId = new ConcurrentHashMap<>();
    private final Map<BlockPos, LockedChest> byLocation = new ConcurrentHashMap<>();

    public ChestService(Database db, int maxChestsPerTeamMember) {
        this.db = db;
        this.maxChestsPerTeamMember = maxChestsPerTeamMember;
    }

    /**
     * Loads all LockedChest records from the database into memory
     */
    public synchronized void loadAll() throws SQLException {
        byTeamId.clear();
        byLocation.clear();
        for (LockedChest c : db.listLockedChests()) {
            index(c);
        }
    }

    /**
     * Indexes a LockedChest record in memory
     */
    private void index(LockedChest c) {
        byTeamId.computeIfAbsent(c.getTeamId(), k -> ConcurrentHashMap.newKeySet())
                .add(c);
        byLocation.put(c.getBlockPos(), c);
    }

    /**
     * Removes a LockedChest record from memory
     */
    private void deindex(LockedChest c) {
        byTeamId.get(c.getTeamId()).remove(c);
        byLocation.remove(c.getBlockPos());
    }

    /**
     * Marks a Chest as locked for a Team
     */
    public synchronized void lockChest(Team team, Block block) throws SQLException {
        LockedChest lockedChest = db.createLockedChest(BlockPos.fromBlock(block), team.getId());
        index(lockedChest);
    }

    /**
     * Marks a already locked chest as unlocked for a Team
     */
    public synchronized void unlockChest(LockedChest chest) throws SQLException {
        db.deleteLockedChest(chest.getBlockPos());
        deindex(chest);
    }

    public synchronized Optional<LockedChest> getLockedChest(Block block) {
        BlockPos pos = BlockPos.fromBlock(block);
        LockedChest chest = byLocation.get(pos);
        if (chest != null) return Optional.of(chest);

        // Look if we have a potential locked counterpart \\
        BlockState state = block.getState();
        if (!(state instanceof Chest chestState)) return Optional.empty();

        // Check if we have a double chest \\
        Inventory holderInventory = chestState.getInventory();
        InventoryHolder holder = holderInventory.getHolder();
        if (!(holder instanceof DoubleChest doubleChest)) return Optional.empty();

        // Get Both Parts \\
        Chest left = (Chest) doubleChest.getLeftSide();
        Chest right = (Chest) doubleChest.getRightSide();

        // Assert that both parts are chests \\
        if (left == null || right == null) return Optional.empty();

        // Get the pos we did not check already \\
        BlockPos leftPos = BlockPos.fromBlock(left.getBlock());
        BlockPos rightPos = BlockPos.fromBlock(right.getBlock());
        BlockPos otherPos = leftPos.equals(pos) ? rightPos : leftPos;

        // Look other pos \\
        return Optional.ofNullable(byLocation.get(otherPos));
    }

    public synchronized boolean checkLockLimit(Team team) {
        return getLockLimit(team) >= getLockedCount(team);
    }
    public synchronized int getLockLimit(Team team) {
        return team.getMembers().size() * maxChestsPerTeamMember;
    }
    public synchronized int getLockedCount(Team team) {
        return byTeamId.getOrDefault(team.getId(), Collections.emptySet()).size();
    }
}
