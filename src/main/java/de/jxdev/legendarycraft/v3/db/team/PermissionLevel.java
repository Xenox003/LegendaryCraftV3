package de.jxdev.legendarycraft.v3.db.team;

import lombok.Getter;

/**
 * Permission levels for team membership, aligned with DB values: 'user', 'admin', 'owner'.
 */
@Getter
public enum PermissionLevel {
    USER("user"),
    ADMIN("admin"),
    OWNER("owner");

    private final String dbValue;

    PermissionLevel(String dbValue) {
        this.dbValue = dbValue;
    }

    public static PermissionLevel fromDb(String value) {
        if (value == null) return null;
        return switch (value.toLowerCase()) {
            case "user" -> USER;
            case "admin" -> ADMIN;
            case "owner" -> OWNER;
            default -> throw new IllegalArgumentException("Unknown permission level: " + value);
        };
    }
}
