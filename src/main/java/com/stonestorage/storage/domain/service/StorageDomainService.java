package com.stonestorage.storage.domain.service;

import com.stonestorage.storage.domain.FileVisibility;
import com.stonestorage.storage.domain.entity.FileMetadata;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StorageDomainService {

    public FileMetadata createMetadata(UUID clientId, String originalName, String systemName, String checksum, long sizeBytes, FileVisibility visibility, String storagePath) {
        return FileMetadata.builder()
                .id(UUID.randomUUID())
                .clientId(clientId)
                .originalName(originalName)
                .systemName(systemName)
                .checksum(checksum)
                .sizeBytes(sizeBytes)
                .visibility(visibility != null ? visibility : FileVisibility.PUBLIC)
                .storagePath(storagePath)
                .build();
    }
}
