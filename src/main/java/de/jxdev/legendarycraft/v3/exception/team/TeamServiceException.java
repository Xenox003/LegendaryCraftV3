package de.jxdev.legendarycraft.v3.exception.team;

import de.jxdev.legendarycraft.v3.exception.TranslatableException;

public class TeamServiceException extends TranslatableException {
    public TeamServiceException(String translationKey) {
        super(translationKey);
    }
}
