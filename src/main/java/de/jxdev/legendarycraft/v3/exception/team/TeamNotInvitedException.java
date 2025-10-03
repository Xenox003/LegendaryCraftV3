package de.jxdev.legendarycraft.v3.exception.team;

public class TeamNotInvitedException extends TeamServiceException {
    public TeamNotInvitedException() {
        super("team.error.not_invited");
    }
}
