package de.jxdev.legendarycraft.v3.service;

import de.jxdev.legendarycraft.v3.data.cache.TeamCache;
import de.jxdev.legendarycraft.v3.data.models.team.*;
import de.jxdev.legendarycraft.v3.data.repository.TeamRepository;
import de.jxdev.legendarycraft.v3.exception.ServiceException;
import de.jxdev.legendarycraft.v3.exception.team.*;
import de.jxdev.legendarycraft.v3.util.TeamUtil;
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

public class TeamServiceImpl implements TeamService {
    private final TeamRepository repo;
    private final TeamCache cache;

    public TeamServiceImpl(TeamRepository repo, TeamCache cache) {
        this.repo = repo;
        this.cache = cache;
    }

    @Override
    public Optional<Team> getTeam(int teamId) {
        try {
            return repo.findById(teamId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
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

    @Override
    public Optional<TeamCacheRecord> getCachedTeam(int teamId) {
        return cache.getTeam(teamId);
    }

    @Override
    public Optional<TeamCacheRecord> getCachedTeamByPlayer(UUID playerId) {
        return cache.getTeamByPlayer(playerId);
    }

    @Override
    public Optional<Integer> getTeamIdByPlayer(UUID playerId) {
        return cache.getTeamId(playerId);
    }

    @Override
    public Team createTeam(String teamName, String prefix, UUID creator) throws TeamServiceException {
        return createTeam(teamName, prefix, NamedTextColor.WHITE, creator);
    }

    @Override
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

            // Update Player prefixes etc \\
            TeamUtil.updateAllPlayerTags(team);
            return team;
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteTeam(TeamCacheRecord team) {
        try {
            TeamUtil.removeAllMemberTags(team.getId());

            repo.deleteTeam(team.getId());
            cache.deIndexTeam(team.getId());
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public List<Team> getAll() {
        try {
            return repo.findAll();
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public List<TeamWithMemberCount> getAllWithMemberCount() {
        try {
            return repo.findAllWithMemberCount();
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void setTeamPrefix(TeamCacheRecord team, String prefix) throws TeamServiceException {
        if (prefix.length() > 15) throw new TeamPrefixTooLongException();

        try {
            repo.updatePrefix(team.getId(), prefix);
            // Prefix is not cached so we do not need to update the cache \\

            // Update Player prefixes etc \\
            Team dbTeam = repo.findById(team.getId()).orElseThrow(TeamNotFoundException::new);
            TeamUtil.updateAllPlayerTags(dbTeam);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void setTeamColor(TeamCacheRecord team, NamedTextColor color) throws TeamServiceException {
        try {
            // Update DB \\
            repo.updateColor(team.getId(), color);

            // Update Cache \\
            Optional<TeamCacheRecord> cachedTeam = cache.getTeam(team.getId());
            cachedTeam.ifPresent(teamCacheRecord -> teamCacheRecord.setColor(color));

            // Update Player prefixes etc \\
            Team dbTeam = repo.findById(team.getId()).orElseThrow(TeamNotFoundException::new);
            TeamUtil.updateAllPlayerTags(dbTeam);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
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

            // No need to update player prefixes cause they do not require team name \\
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void addPlayerToTeam(TeamCacheRecord team, UUID playerId) {
        addPlayerToTeam(team, playerId, TeamMemberRole.MEMBER);
    }

    @Override
    public void addPlayerToTeam(TeamCacheRecord team, UUID playerId, TeamMemberRole role) {
        try {
            repo.addMember(team.getId(), playerId, role);
            cache.indexPlayer(playerId, team.getId());
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public List<TeamMember> getMemberList(TeamCacheRecord team) {
        try {
            return repo.getMemberList(team.getId());
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public int getMemberCount(TeamCacheRecord team) {
        try {
            return repo.getMemberCount(team.getId());
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<TeamMemberRole> getPlayerMemberRole(TeamCacheRecord team, UUID playerId) {
        try {
            return repo.getMemberRole(team.getId(), playerId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isPlayerInTeam(UUID playerId, TeamCacheRecord team) {
        return getPlayerMemberRole(team, playerId).isPresent();
    }

    @Override
    public boolean isPlayerInAnyTeam(UUID playerId) {
        try {
            return repo.findByPlayer(playerId).isPresent();
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isPlayerTeamOwner(UUID playerId, TeamCacheRecord team) {
        return getPlayerMemberRole(team, playerId).filter(role -> role == TeamMemberRole.OWNER).isPresent();
    }

    @Override
    public void removePlayerFromTeam(TeamCacheRecord team, UUID playerId) {
        try {
            repo.removeMember(team.getId(), playerId);
            cache.deIndexPlayer(playerId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void invitePlayerToTeam(TeamCacheRecord team, UUID playerId) {
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

    @Override
    public void acceptInvite(TeamCacheRecord team, UUID playerId) throws TeamServiceException {
        if (getCachedTeamByPlayer(playerId).isPresent())
            throw new TeamAlreadyMemberException();

        try {
            if (!repo.isPlayerInvitedToTeam(playerId, team.getId()))
                throw  new TeamNotInvitedException();

            repo.acceptTeamInvite(team.getId(), playerId);
            cache.indexPlayer(playerId, team.getId());

            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                TeamUtil.updatePlayerTag(player);
            }
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void declineInvite(TeamCacheRecord team, UUID playerId) throws TeamServiceException {
        try {
            if (!repo.isPlayerInvitedToTeam(playerId, team.getId()))
                throw  new TeamNotInvitedException();

            repo.removeInviteFromTeam(team.getId(), playerId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }
}
