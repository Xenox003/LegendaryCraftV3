package de.jxdev.legendarycraft.v3.data.models.discord;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiscordUserLink {
    private String discordId;
    private UUID playerId;
}