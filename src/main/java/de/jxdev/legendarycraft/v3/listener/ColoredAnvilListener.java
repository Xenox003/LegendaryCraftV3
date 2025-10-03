package de.jxdev.legendarycraft.v3.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

public class ColoredAnvilListener implements Listener {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacyAmp = LegacyComponentSerializer.legacyAmpersand();
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        AnvilView inv = event.getView();

        String renameText = inv.getRenameText();
        if (renameText == null || renameText.isEmpty()) return;

        ItemStack left = inv.getItem(0);
        if (left == null || left.getType() == Material.AIR) return;

        ItemStack result = event.getResult();
        result = (result == null || result.getType() == Material.AIR) ? left.clone() : result.clone();

        Component colored = (renameText.indexOf('<') >= 0 ? mm.deserialize(renameText)
                : legacyAmp.deserialize(renameText))
                .decoration(TextDecoration.ITALIC, false);

        String visible = plain.serialize(colored);
        if (visible.length() > 32) {
            String cut = visible.substring(0, 32);
            colored = Component.text(cut).decoration(TextDecoration.ITALIC, false);
        }

        ItemMeta meta = result.getItemMeta();
        meta.displayName(colored);
        result.setItemMeta(meta);
        event.setResult(result);
    }
}
