package de.jxdev.legendarycraft.v3.service;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that manages Player nicknames, prefixes and suffixes
 */
public class PlayerNameServiceImpl implements PlayerNameService {
    private final Map<UUID, Component> prefixes = new ConcurrentHashMap<>();
    private final Map<UUID, Component> suffixes = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastTeamEntryName = new ConcurrentHashMap<>();

    @Override
    public void cleanup(Player player) {
        UUID playerId = player.getUniqueId();
        prefixes.remove(playerId);
        suffixes.remove(playerId);
        String teamName = scoreboardTeamNameFor(player);
        Team team = mainBoard().getTeam(teamName);
        if (team != null) {
            team.unregister();
        }
        lastTeamEntryName.remove(playerId);
    }

    private Scoreboard mainBoard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public PlayerNameServiceImpl() {
    }

    @Override
    public void setNickname(Player player, String nickname) {
        PlayerProfile currentProfile = player.getPlayerProfile();
        PlayerProfile newProfile = Bukkit.createProfileExact(currentProfile.getId(), nickname);
        player.setPlayerProfile(newProfile);

        applyTeam(player);
        applyDisplayAndTab(player);
    }

    @Override
    public void clearNickname(Player player) {
        PlayerProfile profile = player.getPlayerProfile();
        PlayerProfile newProfile = Bukkit.createProfileExact(profile.getId(), player.getName());
        player.setPlayerProfile(profile);

        applyTeam(player);
        applyDisplayAndTab(player);
    }

    @Override
    public void setPrefix(Player player, Component prefix) {
        prefixes.put(player.getUniqueId(), prefix);

        applyTeam(player);
        applyDisplayAndTab(player);
    }

    @Override
    public void clearPrefix(Player player) {
        prefixes.remove(player.getUniqueId());

        applyTeam(player);
        applyDisplayAndTab(player);
    }

    @Override
    public void setSuffix(Player player, Component suffix) {
        suffixes.put(player.getUniqueId(), suffix);

        applyTeam(player);
        applyDisplayAndTab(player);
    }

    @Override
    public void clearSuffix(Player player) {
        suffixes.remove(player.getUniqueId());

        applyTeam(player);
        applyDisplayAndTab(player);
    }

    @Override
    public void refreshEverywhere(Player player) {
        updateTeamEntry(player);
        applyTeam(player);
        applyDisplayAndTab(player);
    }

    private String scoreboardTeamNameFor(Player player) {
        return "pns_" + player.getUniqueId().toString().replace("-", "").substring(0, 16);
    }

    private Team getOrCreateScoreboardTeam(Player player) {
        Scoreboard board = mainBoard();
        String name = scoreboardTeamNameFor(player);
        Team t = board.getTeam(name);
        if (t == null) {
            t = board.registerNewTeam(name);
        }
        return t;
    }

    private void applyTeam(Player p) {
        Team t = getOrCreateScoreboardTeam(p);

        Component prefix = prefixes.getOrDefault(p.getUniqueId(), Component.empty());
        Component suffix = suffixes.getOrDefault(p.getUniqueId(), Component.empty());

        t.prefix(prefix);
        t.suffix(suffix);

        updateTeamEntry(p);
    }

    private void updateTeamEntry(Player p) {
        Team team = getOrCreateScoreboardTeam(p);
        String currentEntry = p.getName();
        String lastEntry = lastTeamEntryName.get(p.getUniqueId());

        if (lastEntry != null && !lastEntry.equals(currentEntry)) {
            team.removeEntry(lastEntry);
        }
        if (!team.hasEntry(currentEntry)) {
            team.addEntry(currentEntry);
        }
        lastTeamEntryName.put(p.getUniqueId(), currentEntry);
    }

    private void applyDisplayAndTab(Player p) {
        Component prefix = prefixes.getOrDefault(p.getUniqueId(), Component.empty());
        Component suffix = suffixes.getOrDefault(p.getUniqueId(), Component.empty());

        Component baseName = Component.text(p.getName());
        Component composite = Component.empty()
                .append(prefix)
                .append(baseName)
                .append(suffix);

        p.displayName(composite);
        p.playerListName(composite);
    }
    
    
}
