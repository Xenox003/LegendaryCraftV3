package de.jxdev.legendarycraft.v3.exception.team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TeamNotFoundException extends TeamServiceException {
    private final String teamName;
    public TeamNotFoundException(String teamName) {
        super("team.error.no_team_with_name");
        this.teamName = teamName;
    }

    public TeamNotFoundException() {
        super("team.error.team_not_found");
        teamName = "";
    }

    @Override
    public Component getChatComponent() {
        return Component.translatable(errorTranslationKey, Component.text(teamName));
    }
}
