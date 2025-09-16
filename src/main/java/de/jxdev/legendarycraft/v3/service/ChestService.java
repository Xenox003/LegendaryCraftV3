package de.jxdev.legendarycraft.v3.service;

import de.jxdev.legendarycraft.v3.data.models.BlockPos;
import de.jxdev.legendarycraft.v3.data.models.LockedChest;

import java.util.List;
import java.util.Optional;

public interface ChestService {
    List<LockedChest> getAllByTeam(int teamId);

    Optional<LockedChest> get(BlockPos pos);

    LockedChest create(int teamId, BlockPos pos);

    void delete(LockedChest chest);

    int getChestLimit(int teamId);

    int getChestCount(int teamId);

    boolean checkChestLimit(int teamId);
}
