package de.jxdev.legendarycraft.v3.discord;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.service.DiscordService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.util.Optional;
import java.util.UUID;

public class SlashCommandListener extends ListenerAdapter {
    private final LegendaryCraft plugin;
    private final DiscordService discordService;

    public SlashCommandListener(LegendaryCraft plugin, DiscordService discordService) {
        this.plugin = plugin;
        this.discordService = discordService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("link")) {
            String code = event.getOption("code") != null ? event.getOption("code").getAsString() : null;
            String discordId = event.getUser().getId();
            try {
                var result = LegendaryCraft.getInstance().getLinkService().linkDiscordWithCode(discordId, code);
                if (result.isPresent()) {
                    event.reply("Erfolgreich verlinkt.").setEphemeral(true).queue();
                } else {
                    event.reply("Ungültiger oder abgelaufener Code.").setEphemeral(true).queue();
                }
            } catch (Exception e) {
                event.reply("Interner Fehler beim Verlinken.").setEphemeral(true).queue();
            }
            return;
        }

        if (event.getName().equals("linked")) {
            // Determine target user (optional user option, defaults to invoker)
            User target = event.getOption("user") != null ? event.getOption("user").getAsUser() : event.getUser();
            String targetDiscordId = target.getId();

            event.deferReply(false).queue();

            // Query link on JDA thread (DB only), but use Bukkit API on main thread for OfflinePlayer
            Bukkit.getScheduler().runTask(LegendaryCraft.getInstance(), () -> {
                try {
                    Optional<UUID> linked = LegendaryCraft.getInstance().getLinkService().getLinkedPlayerByDiscord(targetDiscordId);

                    if (linked.isEmpty()) {
                        event.getHook().editOriginal("Dieser Discord-Nutzer ist nicht mit einem Minecraft-Account verlinkt.").queue();
                        return;
                    }

                    UUID mcUuid = linked.get();
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(mcUuid);
                    String mcName = offlinePlayer.getName() != null ? offlinePlayer.getName() : mcUuid.toString();

                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Verlinkter Account");
                    eb.setColor(new Color(0x2F3136));
                    eb.setThumbnail("https://crafatar.com/avatars/" + mcUuid);
                    eb.addField("Discord", "<@" + targetDiscordId + ">", false);
                    eb.addField("Minecraft", "Name: `" + mcName + "`\nUUID: `" + mcUuid + "`", false);

                    // Fetch playtime and join count
                    long playtimeMs = LegendaryCraft.getInstance().getPlayerStatsService().getTotalPlaytimeMsIncludingActive(mcUuid);
                    int joins = LegendaryCraft.getInstance().getPlayerStatsService().getJoinCount(mcUuid);
                    String playtimeFormatted = de.jxdev.legendarycraft.v3.service.PlayerStatsService.formatDuration(playtimeMs);
                    eb.addField("Statistiken", "Spielzeit: `" + playtimeFormatted + "`\nJoins: `" + joins + "`", false);

                    // Fetch Team \\
                    var team = LegendaryCraft.getInstance().getTeamService().getTeamByPlayer(offlinePlayer.getUniqueId());
                    if (team.isPresent()) {
                        var linkedRole = LegendaryCraft.getInstance().getDiscordTeamRoleRepository().findRoleIdByTeamId(team.get().getId());

                        StringBuilder teamString = new StringBuilder();
                        teamString.append(team.get().getName());
                        linkedRole.ifPresent(s -> teamString.append(" (<@&").append(s).append(">)"));

                        eb.addField("Team", teamString.toString(), false);
                    }

                    event.getHook().editOriginalEmbeds(eb.build()).queue();
                } catch (Exception ex) {
                    event.getHook().editOriginal("Interner Fehler beim Abrufen der Verknüpfung.").queue();
                }
            });
        }
    }
}
