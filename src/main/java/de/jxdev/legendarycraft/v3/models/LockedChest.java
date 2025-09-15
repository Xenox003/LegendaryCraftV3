package de.jxdev.legendarycraft.v3.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LockedChest {
    private int teamId;
    private BlockPos blockPos;
}
