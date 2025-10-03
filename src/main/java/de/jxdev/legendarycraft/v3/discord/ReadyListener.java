package de.jxdev.legendarycraft.v3.discord;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

public class ReadyListener extends ListenerAdapter {
    @Override public void onReady(ReadyEvent event) {
        // Switch to the server thread before touching Bukkit API and update presence:
        Bukkit.getScheduler().runTask(LegendaryCraft.getInstance(), () -> {
            LegendaryCraft plugin = LegendaryCraft.getInstance();
            plugin.getLogger().info("JDA is connected as " + event.getJDA().getSelfUser().getAsTag());
            plugin.updateDiscordPresence();
        });
    }
}
