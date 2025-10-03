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
        return this.cache.get(pos);
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
