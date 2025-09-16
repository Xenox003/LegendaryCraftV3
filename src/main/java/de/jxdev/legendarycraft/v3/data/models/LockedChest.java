package de.jxdev.legendarycraft.v3.data.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LockedChest {
    private int teamId;
    private BlockPos blockPos;
}
