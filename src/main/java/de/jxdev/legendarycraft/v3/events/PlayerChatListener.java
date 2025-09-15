package de.jxdev.legendarycraft.v3.events;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerChatListener implements Listener {
    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            if (!(viewer instanceof Player)) return message;

            return Component.empty()
                    .append(sourceDisplayName.color(NamedTextColor.WHITE))
                    .append(Component.text(" » "))
                    .append(message)
                    .color(NamedTextColor.GRAY);

        });
    }
}
