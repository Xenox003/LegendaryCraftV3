package de.jxdev.legendarycraft.v3.db.team;

/**
 * Permission levels for team membership, aligned with DB values: 'user', 'admin', 'owner'.
 */
public enum PermissionLevel {
    USER("user"),
    ADMIN("admin"),
    OWNER("owner");

    private final String dbValue;

    PermissionLevel(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static PermissionLevel fromDb(String value) {
        if (value == null) return null;
        switch (value.toLowerCase()) {
            case "user":
                return USER;
            case "admin":
                return ADMIN;
            case "owner":
                return OWNER;
            default:
                throw new IllegalArgumentException("Unknown permission level: " + value);
        }
    }
}
