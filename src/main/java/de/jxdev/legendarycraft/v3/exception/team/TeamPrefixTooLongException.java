package de.jxdev.legendarycraft.v3.exception.team;

public class TeamPrefixTooLongException extends TeamServiceException {
    public TeamPrefixTooLongException() {
        super("team.error.prefix_too_long");
    }
}
