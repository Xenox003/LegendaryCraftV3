package de.jxdev.legendarycraft.v3.playerlist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;

import java.time.format.TextStyle;

public class PlayerListComponents {
    public static Component getPlayerListHeader() {
        return Component.text("")
                .append(Component.text(" LegendaryCraft V3 \n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" Powered by jxdev.de \n", NamedTextColor.DARK_GRAY))
                .append(Component.text(""));
    }

    public static Component getPlayerListFooter() {
        return Component.text("\n Aktuell " + Bukkit.getServer().getOnlinePlayers().size() + " online \n")
                .append(Component.text(" TPS: ")
                        .append(getTpsComponent())
                        .append(Component.text(" "))
                )
                .color(NamedTextColor.DARK_GRAY);
    }

    public static Component getTpsComponent() {
        double tps = Bukkit.getServer().getTPS()[0];
        NamedTextColor color = tps > 18.0D ? NamedTextColor.GREEN : (tps > 15.0D ? NamedTextColor.YELLOW : NamedTextColor.RED);
        return Component.text(String.format("%.2f", tps), color);
    }

    public static void updateGlobalPlayerlist() {
        Bukkit.getServer().sendPlayerListHeaderAndFooter(getPlayerListHeader(), getPlayerListFooter());
    }
}
