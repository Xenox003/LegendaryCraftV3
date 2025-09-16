package de.jxdev.legendarycraft.v3.service;

import de.jxdev.legendarycraft.v3.data.models.team.*;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamService {
    public Optional<Team> getTeam(int teamId);
    public Optional<Team> getTeam(String teamName);
    public Optional<Team> getTeamByPlayer(UUID playerId);

    public Optional<TeamCacheRecord> getCachedTeam(int teamId);
    public Optional<TeamCacheRecord> getCachedTeamByPlayer(UUID playerId);
    public Optional<Integer> getTeamIdByPlayer(UUID playerId);

    public Team createTeam(String teamName, String prefix, UUID creator);
    public Team createTeam(String teamName, String prefix, NamedTextColor color, UUID creator);
    public void deleteTeam(int teamId);

    List<Team> getAll();

    List<TeamWithMemberCount> getAllWithMemberCount();

    public void setTeamPrefix(int teamId, String prefix);
    public void setTeamColor(int teamId, NamedTextColor color);
    public void setTeamName(int teamId, String teamName);

    public void addPlayerToTeam(int teamId, UUID playerId);
    public void addPlayerToTeam(int teamId, UUID playerId, TeamMemberRole role);

    List<TeamMember> getMemberList(int teamId);
    int getMemberCount(int teamId);

    Optional<TeamMemberRole> getPlayerMemberRole(int teamId, UUID playerId);

    boolean isPlayerInTeam(UUID playerId, int teamId);

    boolean isPlayerInAnyTeam(UUID playerId);

    boolean isPlayerTeamOwner(UUID playerId, int teamId);

    public void removePlayerFromTeam(int teamId, UUID playerId);

    public void invitePlayerToTeam(int teamId, UUID playerId);
    public void acceptInvite(int teamId, UUID playerId);
    public void declineInvite(int teamId, UUID playerId);
}
