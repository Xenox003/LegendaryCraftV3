package de.jxdev.legendarycraft.v3.data.cache;

import de.jxdev.legendarycraft.v3.data.models.team.TeamCacheRecord;
import de.jxdev.legendarycraft.v3.data.models.team.Team;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeamCache {
    ConcurrentHashMap<UUID, Integer> teamIdByPlayer = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, TeamCacheRecord> teamsById = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, TeamCacheRecord> teamsByName = new ConcurrentHashMap<>();

    /**
     * Adds a player and their TeamId to the cache
     * @param playerId UUID of the player
     * @param teamId ID of the player's team
     */
    public void indexPlayer(UUID playerId, int teamId) {
        teamIdByPlayer.put(playerId, teamId);
    }

    /**
     * Removes a player from the cache
     * @param playerId UUID of the player
     */
    public void deIndexPlayer(UUID playerId) {
        teamIdByPlayer.remove(playerId);
    }

    /**
     * Adds a teamCacheRecord to the cache
     * @param team team to index
     */
    public void indexTeam(Team team) {
        teamsById.put(team.getId(), TeamCacheRecord.fromTeam(team));
        teamsByName.put(team.getName(), TeamCacheRecord.fromTeam(team));
    }

    public void updateNameIndex(TeamCacheRecord record) {
        teamsByName.entrySet().removeIf(e -> e.getValue().getId() == record.getId());
        teamsByName.put(record.getName(), record);
    }

    /**
     * Removes a teamCacheRecord from the cache
     * @param teamId teamId to deindex
     */
    public void deIndexTeam(int teamId) {
        TeamCacheRecord record = teamsById.remove(teamId);
        if (record != null) teamsByName.remove(record.getName());
        teamIdByPlayer.entrySet().removeIf(e -> e.getValue() == teamId);
    }

    public Optional<TeamCacheRecord> getTeam(int teamId) {
        return Optional.ofNullable(teamsById.get(teamId));
    }

    public Optional<TeamCacheRecord> getTeam(String teamName) {
        return Optional.ofNullable(teamsByName.get(teamName));
    }

    public Set<TeamCacheRecord> getAll() {
        return new HashSet<>(teamsById.values());
    }

    public Optional<Integer> getTeamId(UUID playerId) {
        return Optional.ofNullable(teamIdByPlayer.get(playerId));
    }

    public Optional<TeamCacheRecord> getTeamByPlayer(UUID playerId) {
        return getTeamId(playerId).flatMap(this::getTeam);
    }

}
