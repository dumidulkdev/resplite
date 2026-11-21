package dev.dumidu.resplite.config;

public enum AofFsyncPolicy {
    ALWAYS,
    EVERYSEC,
    NO;

    public static AofFsyncPolicy fromString(String value) {
        if (value == null) {
            return EVERYSEC;
        }
        return switch (value.trim().toLowerCase()) {
            case "always" -> ALWAYS;
            case "everysec" -> EVERYSEC;
            case "no" -> NO;
            default -> EVERYSEC;
        };
    }

    public String wireName() {
        return switch (this) {
            case ALWAYS -> "always";
            case EVERYSEC -> "everysec";
            case NO -> "no";
        };
    }
}