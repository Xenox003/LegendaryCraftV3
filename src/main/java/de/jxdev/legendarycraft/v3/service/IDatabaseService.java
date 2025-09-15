package de.jxdev.legendarycraft.v3.service;

import de.jxdev.legendarycraft.v3.models.Team;
import de.jxdev.legendarycraft.v3.models.TeamInvite;
import de.jxdev.legendarycraft.v3.models.TeamMember;
import de.jxdev.legendarycraft.v3.models.TeamMemberRole;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface IDatabaseService {

    /**
     * Creates a new team.
     */
    Team createTeam(String name, String prefix, Player owner, NamedTextColor color) throws SQLException;

    /**
     * Updates a team.
     */
    void updateTeam(Team team) throws SQLException;

    /**
     * Deletes a team.
     */
    void deleteTeam(Team team) throws SQLException;

    /**
     * Get a list of Members of a Team
     */
    Set<TeamMember> getTeamMembers(int teamId) throws SQLException;

    /**
     * Get a list of Team members mapped by Team ID
     */
    Map<Integer, Set<TeamMember>> getTeamMembersForAllTeams() throws SQLException;

    /**
     * Add a player to a team
     */
    void addTeamMember(int teamId, UUID playerId, TeamMemberRole role) throws SQLException;

    /**
     * Remove a player from its team
     */
    void removeTeamMember(UUID playerId) throws SQLException;

    /**
     * Invites a player to a team
     */
    void addTeamInvite(int teamId, UUID playerId) throws SQLException;

    /**
     * Removes a player from a team's invite list.
     */
    void removeTeamInvite(int teamId, UUID playerId) throws SQLException;

    /**
     * Gets a Set of TeamInvites for a player.
     */
    Set<TeamInvite> getTeamInvitesForPlayer(UUID playerId) throws SQLException;

    /**
     * Gets a Set of TeamInvites of a team.
     */
    Set<TeamInvite> getTeamInvitesForTeam(int teamId) throws SQLException;

    /**
     * Gets a list of Team invites mapped by Team ID.
     */
    Map<Integer, Set<TeamInvite>> getTeamInvitesForAllTeams() throws SQLException;
}
