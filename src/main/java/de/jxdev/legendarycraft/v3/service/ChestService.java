package de.jxdev.legendarycraft.v3.service;

import de.jxdev.legendarycraft.v3.data.models.BlockPos;
import de.jxdev.legendarycraft.v3.data.models.LockedChest;
import de.jxdev.legendarycraft.v3.data.models.team.TeamCacheRecord;
import de.jxdev.legendarycraft.v3.exception.team.TeamServiceException;

import java.util.List;
import java.util.Optional;

public interface ChestService {
    List<LockedChest> getAllByTeam(TeamCacheRecord team);

    Optional<LockedChest> get(BlockPos pos);

    LockedChest create(TeamCacheRecord team, BlockPos pos);

    void delete(LockedChest chest);

    int getChestLimit(TeamCacheRecord team);

    int getChestCount(TeamCacheRecord team);

    boolean checkChestLimit(TeamCacheRecord team);
}
