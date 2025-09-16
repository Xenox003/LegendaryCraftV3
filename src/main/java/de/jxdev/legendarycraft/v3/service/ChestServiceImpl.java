package de.jxdev.legendarycraft.v3.service;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.data.cache.LockedChestCache;
import de.jxdev.legendarycraft.v3.data.models.BlockPos;
import de.jxdev.legendarycraft.v3.data.models.LockedChest;
import de.jxdev.legendarycraft.v3.data.repository.LockedChestRepository;
import de.jxdev.legendarycraft.v3.exception.ServiceException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ChestServiceImpl implements ChestService {
    private final LockedChestRepository repo;
    private final LockedChestCache cache;
    private final int maxChestsPerTeamMember;

    public ChestServiceImpl(LockedChestRepository repo, LockedChestCache cache, int maxChestsPerTeamMember) {
        this.repo = repo;
        this.cache = cache;
        this.maxChestsPerTeamMember = maxChestsPerTeamMember;
    }

    @Override
    public List<LockedChest> getAllByTeam(int teamId) {
        try {
            return repo.findByTeam(teamId);
        } catch (SQLException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    @Override
    public Optional<LockedChest> get(BlockPos pos) {
        return this.cache.get(pos);
    }

    @Override
    public LockedChest create(int teamId, BlockPos pos) {
        try {
            LockedChest chest = new LockedChest(teamId, pos);

            repo.create(teamId, pos);
            cache.index(chest);

            return chest;
        } catch (SQLException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    @Override
    public void delete(LockedChest chest) {
        try {
            repo.delete(chest);
            cache.deIndex(chest);
        } catch (SQLException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    @Override
    public int getChestLimit(int teamId) {
        return maxChestsPerTeamMember * LegendaryCraft.getInstance().getTeamService().getMemberCount(teamId);
    }

    @Override
    public int getChestCount(int teamId) {
        return cache.getByTeam(teamId).size();
    }

    @Override
    public boolean checkChestLimit(int teamId) {
        return getChestCount(teamId) < getChestLimit(teamId);
    }
}
