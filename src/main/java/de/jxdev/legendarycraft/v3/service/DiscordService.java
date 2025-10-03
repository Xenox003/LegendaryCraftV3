package de.jxdev.legendarycraft.v3.service;

import com.destroystokyo.paper.event.brigadier.AsyncPlayerSendCommandsEvent;
import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.discord.LinkCommandListener;
import de.jxdev.legendarycraft.v3.discord.ReadyListener;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

public class DiscordService {
    private final LegendaryCraft plugin;

    @Getter
    private JDA jda;

    public DiscordService(LegendaryCraft plugin) {
        this.plugin = plugin;

        JDALogger.setFallbackLoggerEnabled(false);

        // Init JDA in separate Thread \\
        CompletableFuture.runAsync(() -> {
            try {
                EnumSet<GatewayIntent> intents = EnumSet.of(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS
                );

                this.jda = JDABuilder.createDefault(plugin.getConfig().getString("discord.access_token"), intents)
                        .disableCache(net.dv8tion.jda.api.utils.cache.CacheFlag.SCHEDULED_EVENTS,
                                net.dv8tion.jda.api.utils.cache.CacheFlag.EMOJI)
                        .addEventListeners(new ReadyListener(plugin, this))
                        .addEventListeners(new LinkCommandListener(plugin, this))
                        .build();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to start JDA", e);
                // Disable plugin on the main thread to be safe:
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getServer().getPluginManager().disablePlugin(plugin));
            }
        });

        // Discord Presence Update Scheduler (every 30s) \\
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateDiscordPresence, 20L, 600L);
    }

    public void disable() {
        if (jda == null) return;
        try {
            jda.shutdown(); // graceful
            jda.awaitShutdown(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        } finally {
            if (jda.getStatus() != JDA.Status.SHUTDOWN) {
                jda.shutdownNow(); // force if still alive
            }
        }
    }

    public void updateDiscordPresence() {
        if (jda == null) return;

        try {
            String text = String.format("%d Spieler online", Bukkit.getOnlinePlayers().size());
            jda.getPresence().setActivity(Activity.customStatus(text));
        } catch (Throwable t) {
            plugin.getLogger().log(Level.FINE, "Failed to update Discord presence", t);
        }
    }
}
