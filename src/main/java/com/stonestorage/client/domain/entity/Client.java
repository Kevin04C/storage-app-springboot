package com.stonestorage.client.domain.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class Client {

    private final UUID id;
    private final String appName;
    private final String apiKey;
    private final String baseDir;
    private final long quotaBytes;
    private long usedBytes;
    private final boolean active;
    private final Instant createdAt;

    public void addUsedBytes(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("Bytes to add cannot be negative");
        }
        this.usedBytes += bytes;
    }

    public void subtractUsedBytes(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("Bytes to subtract cannot be negative");
        }
        this.usedBytes = Math.max(0, this.usedBytes - bytes);
    }

    public boolean hasEnoughQuota(long requestedBytes) {
        return this.usedBytes + requestedBytes <= this.quotaBytes;
    }
}
