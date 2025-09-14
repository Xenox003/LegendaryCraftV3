package de.jxdev.legendarycraft.v3.models;

import lombok.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.time.Instant;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    private int id;
    private String name;
    private String prefix;
    private NamedTextColor color;
    private Map<UUID, TeamMemberRole> members = new HashMap<>();
    private Set<UUID> invited = new HashSet<>();
    private Instant createdAt;
    private Instant updatedAt;

    public Component getChatComponent() {
        Style style = Style.style(this.color, TextDecoration.UNDERLINED);
        return Component.text(this.name,style)
                .clickEvent(ClickEvent.runCommand("/team info " + this.name))
                .hoverEvent(Component.text("Klicke um Informationen zu diesem Team anzuzeigen."));
    }

    public Component getPrefixComponent() {
        Style style = Style.style(this.color);
        return Component.empty()
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text(this.prefix, style))
                .append(Component.text("]", NamedTextColor.DARK_GRAY));
    }
}
