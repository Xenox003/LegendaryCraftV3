package de.jxdev.legendarycraft.v3.service;

import de.jxdev.legendarycraft.v3.data.models.team.*;
import de.jxdev.legendarycraft.v3.exception.ServiceException;
import de.jxdev.legendarycraft.v3.exception.team.TeamServiceException;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamService {
    public Optional<Team> getTeam(int teamId) throws ServiceException;
    public Optional<Team> getTeam(String teamName);
    public Optional<Team> getTeamByPlayer(UUID playerId);

    public Optional<TeamCacheRecord> getCachedTeam(int teamId);
    public Optional<TeamCacheRecord> getCachedTeamByPlayer(UUID playerId);
    public Optional<Integer> getTeamIdByPlayer(UUID playerId);

    public Team createTeam(String teamName, String prefix, UUID creator) throws TeamServiceException;
    public Team createTeam(String teamName, String prefix, NamedTextColor color, UUID creator) throws TeamServiceException;
    public void deleteTeam(TeamCacheRecord team) throws TeamServiceException;

    List<Team> getAll();
    List<TeamWithMemberCount> getAllWithMemberCount();

    public void setTeamPrefix(TeamCacheRecord team, String prefix) throws TeamServiceException;
    public void setTeamColor(TeamCacheRecord team, NamedTextColor color) throws TeamServiceException;
    public void setTeamName(TeamCacheRecord team, String teamName) throws TeamServiceException;

    public void addPlayerToTeam(TeamCacheRecord team, UUID playerId) throws TeamServiceException;
    public void addPlayerToTeam(TeamCacheRecord team, UUID playerId, TeamMemberRole role) throws TeamServiceException;

    List<TeamMember> getMemberList(TeamCacheRecord team);
    int getMemberCount(TeamCacheRecord team);

    Optional<TeamMemberRole> getPlayerMemberRole(TeamCacheRecord team, UUID playerId);

    boolean isPlayerInTeam(UUID playerId, TeamCacheRecord team) throws TeamServiceException;

    boolean isPlayerInAnyTeam(UUID playerId);

    boolean isPlayerTeamOwner(UUID playerId, TeamCacheRecord team);

    public void removePlayerFromTeam(TeamCacheRecord team, UUID playerId) throws TeamServiceException;

    public void invitePlayerToTeam(TeamCacheRecord team, UUID playerId) throws TeamServiceException;
    public void acceptInvite(TeamCacheRecord team, UUID playerId) throws TeamServiceException;
    public void declineInvite(TeamCacheRecord team, UUID playerId) throws TeamServiceException;
}
