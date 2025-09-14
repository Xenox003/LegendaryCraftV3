package de.jxdev.legendarycraft.v3.teams;

import de.jxdev.legendarycraft.v3.db.Database;
import de.jxdev.legendarycraft.v3.entities.Team;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TeamRepository {
    private final Database db;

    private final Map<Integer, Team> byId = new ConcurrentHashMap<>();
    private final Map<String, Team> byName = new ConcurrentHashMap<>(); // lowercased
    private final Map<UUID, Integer> memberToTeam = new ConcurrentHashMap<>();

    public TeamRepository(Database db) {
        this.db = db;
    }

    public synchronized void loadAll() throws SQLException {
        byId.clear();
        byName.clear();
        memberToTeam.clear();
        for (Team t : db.listTeams()) {
            index(t);
        }
    }

    private void index(Team t) {
        byId.put(t.getId(), t);
        byName.put(t.getName().toLowerCase(Locale.ROOT), t);
        for (UUID m : t.getMembers()) {
            memberToTeam.put(m, t.getId());
        }
    }

    private void deindex(Team t) {
        byId.remove(t.getId());
        byName.remove(t.getName().toLowerCase(Locale.ROOT));
        for (UUID m : t.getMembers()) {
            memberToTeam.remove(m);
        }
    }

    public Optional<Team> getByName(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(byName.get(name.toLowerCase(Locale.ROOT)));
    }

    public Optional<Team> getById(int id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<Team> getAll() {
        return new ArrayList<>(byId.values());
    }

    public synchronized Team createTeam(String name, String prefix, String color, UUID owner) throws SQLException {
        if (byName.containsKey(name.toLowerCase(Locale.ROOT))) {
            throw new SQLException("Team name already exists: " + name);
        }
        Team created = db.createTeam(name, prefix, color, owner);
        index(created);
        return created;
    }

    public synchronized void deleteTeam(String name) throws SQLException {
        Team t = getByName(name).orElseThrow(() -> new SQLException("Team not found: " + name));
        db.deleteTeam(t.getId());
        deindex(t);
    }

    public synchronized void renameTeam(String oldName, String newName) throws SQLException {
        Team t = getByName(oldName).orElseThrow(() -> new SQLException("Team not found: " + oldName));
        if (!oldName.equalsIgnoreCase(newName) && byName.containsKey(newName.toLowerCase(Locale.ROOT))) {
            throw new SQLException("Team name already exists: " + newName);
        }
        byName.remove(t.getName().toLowerCase(Locale.ROOT));
        t.setName(newName);
        db.updateTeam(t);
        byName.put(newName.toLowerCase(Locale.ROOT), t);
    }

    public synchronized void updatePrefix(String name, String prefix) throws SQLException {
        Team t = getByName(name).orElseThrow(() -> new SQLException("Team not found: " + name));
        t.setPrefix(prefix);
        db.updateTeam(t);
    }

    public synchronized void updateColor(String name, String color) throws SQLException {
        Team t = getByName(name).orElseThrow(() -> new SQLException("Team not found: " + name));
        t.setColor(color);
        db.updateTeam(t);
    }

    public synchronized Optional<Team> getByMember(UUID player) {
        Integer teamId = memberToTeam.get(player);
        return teamId == null ? Optional.empty() : getById(teamId);
    }

    public synchronized void addMember(String teamName, UUID player) throws SQLException {
        Team t = getByName(teamName).orElseThrow(() -> new SQLException("Team not found: " + teamName));
        db.addMember(player, t.getId());
        t.getMembers().add(player);
        memberToTeam.put(player, t.getId());
    }

    public synchronized void removeMember(UUID player) throws SQLException {
        Optional<Team> team = getByMember(player);
        db.removeMember(player);
        team.ifPresent(t -> t.getMembers().remove(player));
        memberToTeam.remove(player);
    }
}
