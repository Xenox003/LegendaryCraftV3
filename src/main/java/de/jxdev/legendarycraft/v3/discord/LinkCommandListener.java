package de.jxdev.legendarycraft.v3.discord;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.service.DiscordService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class LinkCommandListener extends ListenerAdapter {
    private final LegendaryCraft plugin;
    private final DiscordService discordService;

    public LinkCommandListener(LegendaryCraft plugin, DiscordService discordService) {
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
        }
    }
}
