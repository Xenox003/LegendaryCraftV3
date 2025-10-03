package de.jxdev.legendarycraft.v3.data.models.discord;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiscordLinkCode {
    private String code;
    private UUID playerId;
    private Instant expires;
    private Instant created;
}