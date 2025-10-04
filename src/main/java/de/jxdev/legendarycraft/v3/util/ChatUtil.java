package de.jxdev.legendarycraft.v3.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class ChatUtil {
    public static Component getTeamChatComponent(Player player, Component message) {
        return Component.text("[Team] ", NamedTextColor.AQUA)
                .append(player.displayName().color(NamedTextColor.WHITE))
                .append(Component.text(" » "))
                .append(message.color(NamedTextColor.WHITE));
    }
}
