package de.jxdev.legendarycraft.v3.exception.team;

public class TeamAlreadyMemberException extends TeamServiceException {
    public TeamAlreadyMemberException() {
        super("team.error.already_member");
    }
}
