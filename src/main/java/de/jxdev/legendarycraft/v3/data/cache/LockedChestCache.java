package de.jxdev.legendarycraft.v3.data.cache;

import de.jxdev.legendarycraft.v3.data.models.BlockPos;
import de.jxdev.legendarycraft.v3.data.models.LockedChest;
import lombok.Locked;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

public class LockedChestCache {
    private final ConcurrentHashMap<BlockPos, LockedChest> byPos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Set<LockedChest>> byTeam = new ConcurrentHashMap<>();

    public void index(LockedChest chest) {
        byPos.put(chest.getBlockPos(), chest);
        byTeam.computeIfAbsent(chest.getTeamId(), k -> ConcurrentHashMap.newKeySet()).add(chest);
    }
    public void deIndex(LockedChest chest) {
        byPos.remove(chest.getBlockPos());
        byTeam.get(chest.getTeamId()).remove(chest);
    }
    public void deIndexTeam(int teamId) {
        byTeam.remove(teamId);
        byPos.entrySet().removeIf(e -> e.getValue().getTeamId() == teamId);
    }

    public Optional<LockedChest> get(BlockPos pos) {
        return Optional.ofNullable(byPos.get(pos));
    }

    public Set<LockedChest> getByTeam(int teamId) {
        return byTeam.getOrDefault(teamId, ConcurrentHashMap.newKeySet());
    }
}
