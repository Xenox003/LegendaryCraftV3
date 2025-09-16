package de.jxdev.legendarycraft.v3.data.models.team;

import de.jxdev.legendarycraft.v3.util.TeamUtil;
import lombok.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    private int id;
    private String name;
    private String prefix;
    private NamedTextColor color;

    public Component getChatComponent() {
        return TeamUtil.getChatComponent(this.name, this.color);
    }

    public Component getPrefixComponent() {
        Style style = Style.style(this.color);
        return Component.empty()
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text(this.prefix, style))
                .append(Component.text("]", NamedTextColor.DARK_GRAY));
    }
}
