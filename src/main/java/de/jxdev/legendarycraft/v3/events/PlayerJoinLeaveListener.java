package de.jxdev.legendarycraft.v3.events;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.service.PlayerNameService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinLeaveListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerNameService nameService = LegendaryCraft.getInstance().getPlayerNameService();

        //LegendaryCraft.getInstance().getPlayerNameService().setNickname(event.getPlayer(), "lolma_69");
        LegendaryCraft.getInstance().getPlayerNameService().setPrefix(event.getPlayer(), Component.text("yeet! ").color(NamedTextColor.DARK_GRAY));



        event.joinMessage(Component.translatable("common.message.player_join", event.getPlayer().displayName().color(NamedTextColor.WHITE)).color(NamedTextColor.GOLD));
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        event.quitMessage(Component.translatable("common.message.player_leave", event.getPlayer().displayName().color(NamedTextColor.WHITE)).color(NamedTextColor.DARK_RED));
    }

}
