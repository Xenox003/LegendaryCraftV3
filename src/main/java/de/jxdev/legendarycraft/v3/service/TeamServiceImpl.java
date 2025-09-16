package de.jxdev.legendarycraft.v3.service;

import de.jxdev.legendarycraft.v3.data.cache.TeamCache;
import de.jxdev.legendarycraft.v3.data.models.team.*;
import de.jxdev.legendarycraft.v3.data.repository.TeamRepository;
import de.jxdev.legendarycraft.v3.exception.ServiceException;
import de.jxdev.legendarycraft.v3.util.TeamUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

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
    public Team createTeam(String teamName, String prefix, UUID creator) {
        return createTeam(teamName, prefix, NamedTextColor.WHITE, creator);
    }

    @Override
    public Team createTeam(String teamName, String prefix, NamedTextColor color, UUID creator) {
        if (prefix.length() > 10) throw new IllegalArgumentException("Prefix must be at most 10 characters long");

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
    public void deleteTeam(int teamId) {
        try {
            TeamUtil.removeAllMemberTags(teamId);

            repo.deleteTeam(teamId);
            cache.deIndexTeam(teamId);
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
    public void setTeamPrefix(int teamId, String prefix) {
        if (prefix.length() > 10) throw new IllegalArgumentException("Prefix must be at most 10 characters long");

        try {
            repo.updatePrefix(teamId, prefix);
            // Prefix is not cached so we do not need to update the cache \\

            // Update Player prefixes etc \\
            Team team = repo.findById(teamId).orElseThrow(() -> new IllegalStateException("Team not found"));
            TeamUtil.updateAllPlayerTags(team);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void setTeamColor(int teamId, NamedTextColor color) {
        try {
            // Update DB \\
            repo.updateColor(teamId, color);

            // Update Cache \\
            Optional<TeamCacheRecord> cachedTeam = cache.getTeam(teamId);
            cachedTeam.ifPresent(teamCacheRecord -> teamCacheRecord.setColor(color));

            // Update Player prefixes etc \\
            Team team = repo.findById(teamId).orElseThrow(() -> new IllegalStateException("Team not found"));
            TeamUtil.updateAllPlayerTags(team);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void setTeamName(int teamId, String teamName) {
        try {
            // Update DB \\
            repo.updateName(teamId, teamName);

            // Update Cache \\
            Optional<TeamCacheRecord> team = cache.getTeam(teamId);
            team.ifPresent(teamCacheRecord -> teamCacheRecord.setName(teamName));
            team.ifPresent(cache::updateNameIndex);

            // No need to update player prefixes cause they do not require team name \\
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void addPlayerToTeam(int teamId, UUID playerId) {
        addPlayerToTeam(teamId, playerId, TeamMemberRole.MEMBER);
    }

    @Override
    public void addPlayerToTeam(int teamId, UUID playerId, TeamMemberRole role) {
        try {
            repo.addMember(teamId, playerId, role);
            cache.indexPlayer(playerId, teamId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public List<TeamMember> getMemberList(int teamId) {
        try {
            return repo.getMemberList(teamId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public int getMemberCount(int teamId) {
        try {
            return repo.getMemberCount(teamId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<TeamMemberRole> getPlayerMemberRole(int teamId, UUID playerId) {
        try {
            return repo.getMemberRole(teamId, playerId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isPlayerInTeam(UUID playerId, int teamId) {
        return getPlayerMemberRole(teamId, playerId).isPresent();
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
    public boolean isPlayerTeamOwner(UUID playerId, int teamId) {
        return getPlayerMemberRole(teamId, playerId).filter(role -> role == TeamMemberRole.OWNER).isPresent();
    }

    @Override
    public void removePlayerFromTeam(int teamId, UUID playerId) {
        try {
            repo.removeMember(teamId, playerId);
            cache.deIndexPlayer(playerId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void invitePlayerToTeam(int teamId, UUID playerId) {
        try {
            repo.invitePlayerToTeam(teamId, playerId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void acceptInvite(int teamId, UUID playerId) {
        try {
            repo.acceptTeamInvite(teamId, playerId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void declineInvite(int teamId, UUID playerId) {
        try {
            repo.removeInviteFromTeam(teamId, playerId);
        } catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }
}
