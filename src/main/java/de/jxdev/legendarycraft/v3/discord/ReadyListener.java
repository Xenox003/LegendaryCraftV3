package de.jxdev.legendarycraft.v3.discord;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.service.DiscordService;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class ReadyListener extends ListenerAdapter {
    private final LegendaryCraft plugin;
    private final DiscordService discordService;

    public ReadyListener(LegendaryCraft plugin, DiscordService discordService) {
        this.plugin = plugin;
        this.discordService = discordService;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        // Switch to the server thread before touching Bukkit API and update presence:
        Bukkit.getScheduler().runTask(LegendaryCraft.getInstance(), () -> {
            plugin.getLogger().info("JDA is connected as " + event.getJDA().getSelfUser().getAsTag());
            discordService.updateDiscordPresence();
            // Initial sync of team roles
            //LegendaryCraft.getInstance().getDiscordTeamRoleSyncService().initialSyncAllTeams();
        });

        // Init Commands \\
        discordService.getJda().upsertCommand(
                Commands.slash("link", "Verlinkt deinen Minecraft und Discord account")
                        .addOption(OptionType.STRING, "code", "Der Link-Code", true)
        ).queue();

        // Command to show the linked Minecraft account of a Discord user
        discordService.getJda().upsertCommand(
                Commands.slash("linked", "Zeigt den verlinkten Minecraft-Account eines Discord-Nutzers")
                        .addOption(OptionType.USER, "user", "Der Discord-Nutzer (optional)", false)
        ).queue();

        plugin.getLogger().info("Discord Commands initialized.");
    }
}
