package de.jxdev.legendarycraft.v3.service;

import de.jxdev.legendarycraft.v3.data.cache.TeamCache;
import de.jxdev.legendarycraft.v3.data.models.team.*;
import de.jxdev.legendarycraft.v3.data.repository.TeamRepository;
import de.jxdev.legendarycraft.v3.event.EventDispatcher;
import de.jxdev.legendarycraft.v3.event.team.*;
import de.jxdev.legendarycraft.v3.exception.ServiceException;
import de.jxdev.legendarycraft.v3.exception.team.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TeamService {
    private final TeamRepository repo;
    private final TeamCache cache;
    private final EventDispatcher eventDispatcher;

    public TeamService(TeamRepository repo, TeamCache cache, EventDispatcher eventDispatcher) {
        this.repo = repo;
        this.cache = cache;
        this.eventDispatcher = eventDispatcher;
    }

    public Optional<Team> getTeam(int teamId) {
        try {
            return repo.findById(teamId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public Optional<Team> getTeam(String teamName) {
        try {
            return repo.findByName(teamName);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public Optional<Team> getTeamByPlayer(UUID playerId) {
        try {
            return repo.findByPlayer(playerId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public Optional<TeamCacheRecord> getCachedTeam(int teamId) {
        return cache.getTeam(teamId);
    }


    public Optional<TeamCacheRecord> getCachedTeamByPlayer(UUID playerId) {
        return cache.getTeamByPlayer(playerId);
    }


    public Optional<Integer> getTeamIdByPlayer(UUID playerId) {
        return cache.getTeamId(playerId);
    }


    public Team createTeam(String teamName, String prefix, UUID creator) throws TeamServiceException {
        return createTeam(teamName, prefix, NamedTextColor.WHITE, creator);
    }


    public Team createTeam(String teamName, String prefix, NamedTextColor color, UUID creator) throws TeamServiceException {
        if (teamName.length() > 15) throw new TeamNameTooLongException();
        if (getTeam(teamName).isPresent()) throw new TeamNameAlreadyUsedException();
        if (prefix.length() > 15) throw new TeamPrefixTooLongException();

        try {
            // Create in DB \\
            final int teamId = repo.createTeam(teamName, prefix, color, creator);

            // Create Team object \\
            Team team = new Team(teamId, teamName, prefix, color);

            // Create in cache \\
            cache.indexTeam(team);
            cache.indexPlayer(creator, teamId);

            // Dispatch event for team creation \
            eventDispatcher.dispatchEvent(new TeamCreatedEvent(team));
            return team;
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public void deleteTeam(TeamCacheRecord team) throws TeamServiceException {
        try {
            // Dispatch event to allow listeners to clean up player tags etc.
            eventDispatcher.dispatchEvent(new TeamDeletedEvent(team.getId()));

            repo.deleteTeam(team.getId());
            cache.deIndexTeam(team.getId());
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public List<Team> getAll() {
        try {
            return repo.findAll();
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public List<TeamWithMemberCount> getAllWithMemberCount() {
        try {
            return repo.findAllWithMemberCount();
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public void setTeamPrefix(TeamCacheRecord team, String prefix) throws TeamServiceException {
        if (prefix.length() > 15) throw new TeamPrefixTooLongException();

        try {
            repo.updatePrefix(team.getId(), prefix);
            // Prefix is not cached so we do not need to update the cache \

            // Dispatch prefix changed event \
            Team dbTeam = repo.findById(team.getId()).orElseThrow(TeamNotFoundException::new);
            eventDispatcher.dispatchEvent(new TeamPrefixChangedEvent(dbTeam));
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public void setTeamColor(TeamCacheRecord team, NamedTextColor color) throws TeamServiceException {
        try {
            // Update DB \\
            repo.updateColor(team.getId(), color);

            // Update Cache \\
            Optional<TeamCacheRecord> cachedTeam = cache.getTeam(team.getId());
            cachedTeam.ifPresent(teamCacheRecord -> teamCacheRecord.setColor(color));

            // Dispatch color changed event \
            Team dbTeam = repo.findById(team.getId()).orElseThrow(TeamNotFoundException::new);
            eventDispatcher.dispatchEvent(new TeamColorChangedEvent(dbTeam));
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public void setTeamName(TeamCacheRecord team, String teamName) throws TeamServiceException {
        if (teamName.length() > 15) throw new TeamNameTooLongException();
        if (getTeam(teamName).isPresent()) throw new TeamNameAlreadyUsedException();

        try {
            // Update DB \\
            repo.updateName(team.getId(), teamName);

            // Update Cache \\
            Optional<TeamCacheRecord> dbTeam = cache.getTeam(team.getId());
            dbTeam.ifPresent(teamCacheRecord -> teamCacheRecord.setName(teamName));
            dbTeam.ifPresent(cache::updateNameIndex);

            // Dispatch name changed event (no tag update needed currently) \
            eventDispatcher.dispatchEvent(new TeamNameChangedEvent(team.getId(), teamName));
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public void addPlayerToTeam(TeamCacheRecord team, UUID playerId) {
        addPlayerToTeam(team, playerId, TeamMemberRole.MEMBER);
    }


    public void addPlayerToTeam(TeamCacheRecord team, UUID playerId, TeamMemberRole role) {
        try {
            repo.addMember(team.getId(), playerId, role);
            cache.indexPlayer(playerId, team.getId());

            // Dispatch event with DB team instead of cache record \
            Team dbTeam = cache.getDbTeam(team, repo).orElseThrow(() -> new ServiceException("Team not found"));
            eventDispatcher.dispatchEvent(new PlayerAddedToTeamEvent(dbTeam, playerId));
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public List<TeamMember> getMemberList(TeamCacheRecord team) {
        try {
            return repo.getMemberList(team.getId());
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public int getMemberCount(TeamCacheRecord team) {
        try {
            return repo.getMemberCount(team.getId());
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public Optional<TeamMemberRole> getPlayerMemberRole(TeamCacheRecord team, UUID playerId) {
        try {
            return repo.getMemberRole(team.getId(), playerId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public boolean isPlayerInTeam(UUID playerId, TeamCacheRecord team) {
        return getPlayerMemberRole(team, playerId).isPresent();
    }


    public boolean isPlayerInAnyTeam(UUID playerId) {
        try {
            return repo.findByPlayer(playerId).isPresent();
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public boolean isPlayerTeamOwner(UUID playerId, TeamCacheRecord team) {
        return getPlayerMemberRole(team, playerId).filter(role -> role == TeamMemberRole.OWNER).isPresent();
    }


    public void removePlayerFromTeam(TeamCacheRecord team, UUID playerId) throws TeamServiceException {
        try {
            repo.removeMember(team.getId(), playerId);
            cache.deIndexPlayer(playerId);

            // Dispatch event with DB team instead of cache record \
            Team dbTeam = cache.getDbTeam(team, repo).orElseThrow(() -> new ServiceException("Team not found"));
            eventDispatcher.dispatchEvent(new PlayerRemovedFromTeamEvent(dbTeam, playerId));
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public void invitePlayerToTeam(TeamCacheRecord team, UUID playerId) throws TeamServiceException {
        try {
            repo.invitePlayerToTeam(team.getId(), playerId);

            final Player target = Bukkit.getPlayer(playerId);
            if (target != null) {
                target.sendMessage(Component.translatable("team.invite.message",
                                team.getChatComponent(),
                                Component.translatable("team.invite.clickable")
                                        .style(Style.style(NamedTextColor.GREEN, TextDecoration.UNDERLINED))
                                        .clickEvent(ClickEvent.runCommand("/team join " + team.getName()))
                        )
                );
            }
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public void acceptInvite(TeamCacheRecord team, UUID playerId) throws TeamServiceException {
        if (getCachedTeamByPlayer(playerId).isPresent())
            throw new TeamAlreadyMemberException();

        try {
            if (!repo.isPlayerInvitedToTeam(playerId, team.getId()))
                throw new TeamNotInvitedException();

            repo.acceptTeamInvite(team.getId(), playerId);
            cache.indexPlayer(playerId, team.getId());

            // Treat accept as a player being added to the team
            Team dbTeam = cache.getDbTeam(team, repo).orElseThrow(() -> new ServiceException("Team not found"));
            eventDispatcher.dispatchEvent(new PlayerAddedToTeamEvent(dbTeam, playerId));
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public void declineInvite(TeamCacheRecord team, UUID playerId) throws TeamServiceException {
        try {
            if (!repo.isPlayerInvitedToTeam(playerId, team.getId()))
                throw new TeamNotInvitedException();

            repo.removeInviteFromTeam(team.getId(), playerId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }
}
