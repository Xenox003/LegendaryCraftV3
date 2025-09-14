package de.jxdev.legendarycraft.v3.entities;

import lombok.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    private int id;
    private String name;
    private String prefix;
    private String color;
    private UUID ownerUuid;
    private Set<UUID> members = new HashSet<>();
    private Instant createdAt;
    private Instant updatedAt;

    public Component getChatComponent() {
        Style style = Style.style(NamedTextColor.NAMES.value(this.color.toLowerCase()), TextDecoration.UNDERLINED);
        return Component.text(this.name,style)
                .clickEvent(ClickEvent.runCommand("/team info " + this.name));
    }
}
