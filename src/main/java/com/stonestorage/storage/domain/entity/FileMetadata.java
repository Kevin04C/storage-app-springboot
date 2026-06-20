package com.stonestorage.storage.domain.entity;

import com.stonestorage.storage.domain.FileVisibility;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FileMetadata {

    private final UUID id;
    private final UUID clientId;
    private final String originalName;
    private final String systemName;
    private final String checksum;
    private final long sizeBytes;
    private final FileVisibility visibility;
    private final String storagePath;
    private final Instant createdAt;
    private final Instant deletedAt;

    public static FileMetadata create(UUID clientId, String originalName, String systemName,
                                       String checksum, long sizeBytes, FileVisibility visibility,
                                       String storagePath) {
        return FileMetadata.builder()
                .clientId(clientId)
                .originalName(originalName)
                .systemName(systemName)
                .checksum(checksum)
                .sizeBytes(sizeBytes)
                .visibility(visibility != null ? visibility : FileVisibility.PUBLIC)
                .storagePath(storagePath)
                .build();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isPublic() {
        return FileVisibility.PUBLIC == visibility;
    }
}
