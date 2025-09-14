package de.jxdev.legendarycraft.v3.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.OfflinePlayer;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember {
    public OfflinePlayer player;
    public Team team;
    public TeamMemberRole role;
}
