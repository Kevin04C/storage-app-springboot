package com.stonestorage.shared.domain.valueobject;

public record FriendlySize(String value) {

    public static FriendlySize fromBytes(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("Bytes cannot be negative");
        }
        String result;
        if (bytes < 1024) {
            result = bytes + " B";
        } else if (bytes < 1024L * 1024) {
            result = String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024L * 1024 * 1024) {
            result = String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            result = String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
        return new FriendlySize(result);
    }

    public static long toBytes(String friendly) {
        if (friendly == null || friendly.isBlank()) {
            return 0;
        }
        String trimmed = friendly.trim().toUpperCase().replace(" ", "");
        if (trimmed.endsWith("GB")) {
            return parse(trimmed, "GB", 1024L * 1024 * 1024);
        } else if (trimmed.endsWith("MB")) {
            return parse(trimmed, "MB", 1024L * 1024);
        } else if (trimmed.endsWith("KB")) {
            return parse(trimmed, "KB", 1024);
        } else if (trimmed.endsWith("B")) {
            return parse(trimmed, "B", 1);
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid size format: " + friendly);
        }
    }

    private static long parse(String value, String suffix, long multiplier) {
        String numberPart = value.substring(0, value.length() - suffix.length());
        double number = Double.parseDouble(numberPart);
        return (long) (number * multiplier);
    }
}
