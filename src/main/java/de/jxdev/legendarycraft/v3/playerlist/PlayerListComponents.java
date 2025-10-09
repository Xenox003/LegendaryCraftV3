package de.jxdev.legendarycraft.v3.playerlist;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.service.PlayerStatsService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerListComponents {
    public static Component getPlayerListHeader() {
        return Component.text("")
                .append(Component.text(" LegendaryCraft 3 \n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" Powered by jxdev.de \n", NamedTextColor.DARK_GRAY))
                .append(Component.text(""));
    }

    public static Component getPlayerListFooter(Player player) {
        Component onlineLine = Component.text("\n Aktuell " + Bukkit.getServer().getOnlinePlayers().size() + " online \n");
        Component tpsLine = Component.text(" TPS: ")
                .append(getTpsComponent())
                .append(Component.text(" "));
        Component playtimeLine = Component.text("\n Spielzeit: ")
                .append(Component.text(formatPlaytime(player)))
                .append(Component.text(" \n"));

        return Component.empty()
                .append(onlineLine)
                .append(tpsLine)
                .append(playtimeLine)
                .color(NamedTextColor.DARK_GRAY);
    }

    public static Component getTpsComponent() {
        double tps = Bukkit.getServer().getTPS()[0];
        NamedTextColor color = tps > 18.0D ? NamedTextColor.GREEN : (tps > 15.0D ? NamedTextColor.YELLOW : NamedTextColor.RED);
        return Component.text(String.format("%.2f", tps), color);
    }

    private static String formatPlaytime(Player player) {
        long ms = LegendaryCraft.getInstance().getPlayerStatsService().getTotalPlaytimeMsIncludingActive(player.getUniqueId());
        return PlayerStatsService.formatDuration(ms);
    }

    public static void updateGlobalPlayerlist() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendPlayerListHeaderAndFooter(getPlayerListHeader(), getPlayerListFooter(p));
        }
    }
}
