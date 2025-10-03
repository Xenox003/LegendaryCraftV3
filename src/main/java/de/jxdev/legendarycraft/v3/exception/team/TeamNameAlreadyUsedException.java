package de.jxdev.legendarycraft.v3.exception.team;

public class TeamNameAlreadyUsedException extends TeamServiceException {
    public TeamNameAlreadyUsedException() {
        super("team.error.name_already_used");
    }
}
