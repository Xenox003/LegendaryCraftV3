package de.jxdev.legendarycraft.v3.discord;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.data.models.team.Team;
import de.jxdev.legendarycraft.v3.data.models.team.TeamCacheRecord;
import de.jxdev.legendarycraft.v3.data.models.team.TeamMember;
import de.jxdev.legendarycraft.v3.data.repository.DiscordTeamRoleRepository;
import de.jxdev.legendarycraft.v3.data.repository.DiscordUserRepository;
import de.jxdev.legendarycraft.v3.data.repository.TeamRepository;
import de.jxdev.legendarycraft.v3.event.EventListener;
import de.jxdev.legendarycraft.v3.event.team.*;
import de.jxdev.legendarycraft.v3.service.DiscordService;
import de.jxdev.legendarycraft.v3.service.TeamService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.kyori.adventure.text.format.NamedTextColor;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DiscordTeamRoleSyncService {
    private final LegendaryCraft plugin;
    private final DiscordService discordService;
    private final TeamService teamService;
    private final TeamRepository teamRepository;
    private final DiscordTeamRoleRepository roleRepo;
    private final DiscordUserRepository userRepo;

    public DiscordTeamRoleSyncService(LegendaryCraft plugin,
                                      DiscordService discordService,
                                      TeamService teamService,
                                      TeamRepository teamRepository,
                                      DiscordTeamRoleRepository roleRepo,
                                      DiscordUserRepository userRepo) {
        this.plugin = plugin;
        this.discordService = discordService;
        this.teamService = teamService;
        this.teamRepository = teamRepository;
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
    }

    public void initialSyncAllTeams() {
        Guild guild = discordService.getGuild();
        if (guild == null) return;
        try {
            for (Team t : teamService.getAll()) {
                ensureRoleUpToDate(guild, t);
                syncTeamMembers(guild, t);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Discord initial team role sync failed: " + e.getMessage());
        }
    }

    public EventListener<TeamCreatedEvent> onTeamCreated() {
        return event -> {
            Guild guild = discordService.getGuild();
            if (guild == null) return;
            ensureRoleUpToDate(guild, event.getTeam());
        };
    }

    public EventListener<TeamDeletedEvent> onTeamDeleted() {
        return event -> {
            Guild guild = discordService.getGuild();
            if (guild == null) return;
            try {
                int teamId = event.getTeamId();
                Optional<String> roleIdOpt = roleRepo.findRoleIdByTeamId(teamId);
                if (roleIdOpt.isPresent()) {
                    Role role = guild.getRoleById(roleIdOpt.get());
                    if (role != null) {
                        role.delete().queue();
                    }
                    roleRepo.delete(teamId);
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed to delete discord role mapping for team: " + ex.getMessage());
            }
        };
    }

    public EventListener<TeamColorChangedEvent> onTeamColorChanged() {
        return event -> {
            Guild guild = discordService.getGuild();
            if (guild == null) return;
            ensureRoleUpToDate(guild, event.getTeam());
        };
    }

    public EventListener<TeamPrefixChangedEvent> onTeamPrefixChanged() {
        // We use team name for the role, not prefix; nothing to do here.
        return event -> {};
    }

    public EventListener<TeamNameChangedEvent> onTeamNameChanged() {
        return event -> {
            Guild guild = discordService.getGuild();
            if (guild == null) return;
            try {
                int teamId = event.getTeamId();
                Optional<Team> teamOpt = teamRepository.findById(teamId);
                teamOpt.ifPresent(team -> ensureRoleUpToDate(guild, team));
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to fetch team on name change: " + e.getMessage());
            }
        };
    }

    public EventListener<PlayerAddedToTeamEvent> onPlayerAdded() {
        return event -> {
            Guild guild = discordService.getGuild();
            if (guild == null) return;
            grantRoleToPlayer(guild, event.getTeam(), event.getPlayerId());
        };
    }

    public EventListener<PlayerRemovedFromTeamEvent> onPlayerRemoved() {
        return event -> {
            Guild guild = discordService.getGuild();
            if (guild == null) return;
            revokeRoleFromPlayer(guild, event.getTeam(), event.getPlayerId());
        };
    }

    private void syncTeamMembers(Guild guild, Team team) {
        try {
            Optional<String> roleIdOpt = roleRepo.findRoleIdByTeamId(team.getId());
            if (roleIdOpt.isEmpty()) return;
            Role role = guild.getRoleById(roleIdOpt.get());
            if (role == null) return;
            List<TeamMember> members = teamService.getMemberList(team);
            for (TeamMember m : members) {
                grantRoleToPlayer(guild, team, m.getPlayerId());
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to sync team members: " + e.getMessage());
        }
    }

    private void grantRoleToPlayer(Guild guild, TeamCacheRecord team, UUID playerId) {
        try {
            var linkOpt = userRepo.findByPlayerId(playerId);
            if (linkOpt.isEmpty()) return;
            String discordId = linkOpt.get().getDiscordId();
            ensureRoleUpToDate(guild, new Team(team.getId(), team.getName(), "", team.getColor()));
            Optional<String> roleIdOpt = roleRepo.findRoleIdByTeamId(team.getId());
            if (roleIdOpt.isEmpty()) return;
            String roleId = roleIdOpt.get();
            Role role = guild.getRoleById(roleId);
            if (role == null) return;
            guild.retrieveMemberById(discordId).queue(member -> addRoleIfMissing(member, role), err -> {});
        } catch (SQLException e) {
            plugin.getLogger().warning("grantRoleToPlayer failed: " + e.getMessage());
        }
    }

    private void revokeRoleFromPlayer(Guild guild, TeamCacheRecord team, UUID playerId) {
        try {
            var linkOpt = userRepo.findByPlayerId(playerId);
            if (linkOpt.isEmpty()) return;
            String discordId = linkOpt.get().getDiscordId();
            Optional<String> roleIdOpt = roleRepo.findRoleIdByTeamId(team.getId());
            if (roleIdOpt.isEmpty()) return;
            String roleId = roleIdOpt.get();
            Role role = guild.getRoleById(roleId);
            if (role == null) return;
            guild.retrieveMemberById(discordId).queue(member -> removeRoleIfPresent(member, role), err -> {});
        } catch (SQLException e) {
            plugin.getLogger().warning("revokeRoleFromPlayer failed: " + e.getMessage());
        }
    }

    private void addRoleIfMissing(Member member, Role role) {
        if (!member.getRoles().contains(role)) {
            member.getGuild().addRoleToMember(member, role).queue();
        }
    }

    private void removeRoleIfPresent(Member member, Role role) {
        if (member.getRoles().contains(role)) {
            member.getGuild().removeRoleFromMember(member, role).queue();
        }
    }

    private void ensureRoleUpToDate(Guild guild, Team team) {
        try {
            String desiredName = team.getName();
            Color desiredColor = mapColor(team.getColor());
            Optional<String> roleIdOpt = roleRepo.findRoleIdByTeamId(team.getId());
            if (roleIdOpt.isPresent()) {
                Role role = guild.getRoleById(roleIdOpt.get());
                if (role == null) {
                    // recreate if missing
                    guild.createRole().setName(desiredName).setColor(desiredColor).queue(r -> {
                        try { roleRepo.upsert(team.getId(), r.getId()); } catch (SQLException ignored) {}
                    });
                    return;
                }
                boolean needsUpdate = !role.getName().equals(desiredName) || (desiredColor != null && !desiredColor.equals(role.getColor()));
                if (needsUpdate) {
                    role.getManager().setName(desiredName).setColor(desiredColor).queue();
                }
            } else {
                guild.createRole().setName(desiredName).setColor(desiredColor).queue(r -> {
                    try { roleRepo.upsert(team.getId(), r.getId()); } catch (SQLException ignored) {}
                });
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("ensureRoleUpToDate failed: " + e.getMessage());
        }
    }

    private Color mapColor(NamedTextColor c) {
        if (c == null) return null;
        try {
            return new Color(c.value());
        } catch (Throwable t) {
            return null;
        }
    }
}
