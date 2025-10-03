package de.jxdev.legendarycraft.v3.data.models.team;

import de.jxdev.legendarycraft.v3.util.TeamUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamCacheRecord {
    protected int id;
    protected String name;
    protected NamedTextColor color;

    public static TeamCacheRecord fromTeam(Team team) {
        return new TeamCacheRecord(team.getId(), team.getName(), team.getColor());
    }

    public Component getChatComponent() {
        return TeamUtil.getChatComponent(this.name, this.color);
    }
}