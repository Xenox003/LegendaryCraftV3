package de.jxdev.legendarycraft.v3.service;

import de.jxdev.legendarycraft.v3.db.Database;
import de.jxdev.legendarycraft.v3.models.Team;
import de.jxdev.legendarycraft.v3.models.TeamMemberRole;
import net.kyori.adventure.text.format.NamedTextColor;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeamService {
    private final Database db;

    private final Map<Integer, Team> byId = new ConcurrentHashMap<>();
    private final Map<String, Team> byName = new ConcurrentHashMap<>(); // lowercased
    private final Map<UUID, Integer> playerToTeam = new ConcurrentHashMap<>();

    public TeamService(Database db) {
        this.db = db;
    }

    /**
     * Loads all Team records from the database into memory
     */
    public synchronized void loadAll() throws SQLException {
        byId.clear();
        byName.clear();
        playerToTeam.clear();
        for (Team t : db.listTeams()) {
            index(t);
        }
    }

    /**
     * Indexes a Team record in memory
     * @param t Team to index
     */
    private void index(Team t) {
        byId.put(t.getId(), t);
        byName.put(t.getName().toLowerCase(Locale.ROOT), t);
        for (UUID m : t.getMembers().keySet()) {
            playerToTeam.put(m, t.getId());
        }
    }

    /**
     * Removes a Team record from memory
     * @param t Team to remove
     */
    private void deindex(Team t) {
        byId.remove(t.getId());
        byName.remove(t.getName().toLowerCase(Locale.ROOT));
        for (UUID m : t.getMembers().keySet()) {
            playerToTeam.put(m, t.getId());
        }
    }

    /**
     * Gets a Team by name, or empty if not found
     * @param name Name of the Team
     */
    public Optional<Team> getByName(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(byName.get(name.toLowerCase(Locale.ROOT)));
    }

    /**
     * Gets a Team by ID, or empty if not found
     * @param id Id of the Team
     */
    public Optional<Team> getById(int id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Gets a list of all Teams
     */
    public List<Team> getAll() {
        return new ArrayList<>(byId.values());
    }

    /**
     * Creates a new Team record and indexes it in memory
     *
     * @param name   Name of the Team
     * @param prefix Prefix of the Team
     * @param owner  UUID of the Team Owner
     * @param color  OPTIONAL, Color of the Team
     */
    public synchronized Team createTeam(String name, String prefix, UUID owner, @Nullable NamedTextColor color) throws SQLException {
        if (byName.containsKey(name.toLowerCase(Locale.ROOT))) {
            throw new SQLException("Team name already exists: " + name);
        }
        if (prefix.length() > 10) {
            throw new SQLException("Prefix too long: " + prefix);
        }

        NamedTextColor finalColor = color == null ? NamedTextColor.WHITE : color;

        Team created = db.createTeam(name, prefix, finalColor, owner);
        index(created);
        return created;
    }

    /**
     * Deletes a Team record from the database and removes it from memory
     */
    public synchronized void deleteTeam(Team team) throws SQLException {
        db.deleteTeam(team.getId());
        deindex(team);
    }

    /**
     * Changes the name of a Team record
     */
    public synchronized void renameTeam(Team team, String newName) throws SQLException {
        if (byName.containsKey(newName.toLowerCase(Locale.ROOT))) {
            throw new SQLException("Team name already exists: " + newName);
        }
        byName.remove(team.getName().toLowerCase(Locale.ROOT));
        team.setName(newName);
        db.updateTeam(team);
        byName.put(newName.toLowerCase(Locale.ROOT), team);
    }

    /**
     * Changes the prefix of a Team record
     */
    public synchronized void updatePrefix(Team team, String prefix) throws SQLException {
        if (prefix.length() > 10) {
            throw new SQLException("Prefix too long: " + prefix);
        }

        team.setPrefix(prefix);
        db.updateTeam(team);
    }

    /**
     * Changes the color of a Team record
     */
    public synchronized void updateColor(Team team, NamedTextColor color) throws SQLException {
        team.setColor(color);
        db.updateTeam(team);
    }

    /**
     * Gets the Team of a player
     */
    public synchronized Optional<Team> getPlayerTeam(UUID player) {
        Integer teamId = playerToTeam.get(player);
        return teamId == null ? Optional.empty() : getById(teamId);
    }

    /**
     * Adds a player to a Team
     */
    public synchronized void addTeamMember(Team team, UUID player, @Nullable TeamMemberRole role) throws SQLException {
        if (team.getMembers().containsKey(player)) {
            throw new SQLException("Player already in team: " + player);
        }

        TeamMemberRole finalRole = role == null ? TeamMemberRole.MEMBER : role;

        db.addTeamMember(team.getId(), player, role);
        team.getMembers().put(player, role);
        playerToTeam.put(player, team.getId());
    }

    /**
     * Removes a player from its Team
     */
    public synchronized void removeMember(UUID player) throws SQLException {
        Team team = getPlayerTeam(player).orElseThrow(() -> new SQLException("Player not in a team: " + player));
        db.removeTeamMembers(player);
        team.getMembers().remove(player);
        playerToTeam.remove(player);
    }

    /**
     * Invites a player to a Team
     */
    public synchronized void invitePlayerToTeam(Team team, UUID player) throws SQLException {

    }
}
