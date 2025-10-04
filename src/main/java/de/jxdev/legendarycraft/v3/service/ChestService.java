package de.jxdev.legendarycraft.v3.service;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.data.cache.LockedChestCache;
import de.jxdev.legendarycraft.v3.data.models.BlockPos;
import de.jxdev.legendarycraft.v3.data.models.LockedChest;
import de.jxdev.legendarycraft.v3.data.models.team.TeamCacheRecord;
import de.jxdev.legendarycraft.v3.data.repository.LockedChestRepository;
import de.jxdev.legendarycraft.v3.exception.ServiceException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ChestService {
    private final LockedChestRepository repo;
    private final LockedChestCache cache;
    private final int maxChestsPerTeamMember;

    public ChestService(LockedChestRepository repo, LockedChestCache cache, int maxChestsPerTeamMember) {
        this.repo = repo;
        this.cache = cache;
        this.maxChestsPerTeamMember = maxChestsPerTeamMember;
    }

    
    public List<LockedChest> getAllByTeam(TeamCacheRecord team) {
        try {
            return repo.findByTeam(team.getId());
        } catch (SQLException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    
    public Optional<LockedChest> get(BlockPos pos) {
        // First, attempt direct lookup
        Optional<LockedChest> direct = this.cache.get(pos);
        if (direct.isPresent()) return direct;

        // Resolve potential double chest counterpart using Bukkit world + ChestUtil
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(pos.worldId());
        if (world == null) return Optional.empty();

        org.bukkit.block.Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
        // Only proceed if this is a chest block
        if (block.getType() != org.bukkit.Material.CHEST) return Optional.empty();

        // Use ChestUtil to resolve the group (single or double chest)
        java.util.List<org.bukkit.block.Chest> group = de.jxdev.legendarycraft.v3.util.ChestUtil.resolveChestGroup(block);
        for (org.bukkit.block.Chest chestState : group) {
            de.jxdev.legendarycraft.v3.data.models.BlockPos candidate = de.jxdev.legendarycraft.v3.data.models.BlockPos.fromBlock(chestState.getBlock());
            Optional<LockedChest> found = this.cache.get(candidate);
            if (found.isPresent()) return found; // return locked counterpart if exists
        }

        return Optional.empty();
    }

    
    public LockedChest create(TeamCacheRecord team, BlockPos pos) {
        try {
            LockedChest chest = new LockedChest(team.getId(), pos);

            repo.create(team.getId(), pos);
            cache.index(chest);

            return chest;
        } catch (SQLException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    
    public void delete(LockedChest chest) {
        try {
            repo.delete(chest);
            cache.deIndex(chest);
        } catch (SQLException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    
    public int getChestLimit(TeamCacheRecord team) {
        return maxChestsPerTeamMember * LegendaryCraft.getInstance().getTeamService().getMemberCount(team);
    }

    
    public int getChestCount(TeamCacheRecord team) {
        return cache.getByTeam(team.getId()).size();
    }

    
    public boolean checkChestLimit(TeamCacheRecord team) {
        return getChestCount(team) < getChestLimit(team);
    }
}
