package de.jxdev.legendarycraft.v3.listener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public class ColoredSignListener implements Listener {
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        for (int i = 0; i < event.lines().size(); i++) {
            Component rawComp = event.line(i);
            if (rawComp == null) continue;

            String raw = PlainTextComponentSerializer.plainText().serialize(rawComp);
            if (raw.isEmpty()) continue;

            Component colored = (raw.contains("<")
                    ? MiniMessage.miniMessage().deserialize(raw)
                    : LegacyComponentSerializer.legacyAmpersand().deserialize(raw))
                    .decoration(TextDecoration.ITALIC, false);

            String visible = PlainTextComponentSerializer.plainText().serialize(colored);
            if (visible.length() > 15) {
                visible = visible.substring(0, 15);
                colored = Component.text(visible).decoration(TextDecoration.ITALIC, false);
            }

            event.line(i, colored);
        }
    }
}
