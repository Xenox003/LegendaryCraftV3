package de.jxdev.legendarycraft.v3.exception.team;

public class TeamNameTooLongException extends TeamServiceException {
    public TeamNameTooLongException() {
        super("team.error.name_too_long");
    }
}
