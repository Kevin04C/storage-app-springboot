package com.stonestorage.storage.domain.service;

import com.stonestorage.storage.domain.FileVisibility;
import com.stonestorage.storage.domain.entity.FileMetadata;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StorageDomainServiceTest {

    private final StorageDomainService service = new StorageDomainService();

    @Test
    void createMetadata_shouldGenerateValidMetadata() {
        UUID clientId = UUID.randomUUID();
        FileMetadata metadata = service.createMetadata(clientId, "photo.jpg", "photo-a1b2c3d4.jpg", "abc123", 1024, FileVisibility.PRIVATE, "/app1/images/photo-a1b2c3d4.jpg");

        assertNotNull(metadata.getId());
        assertEquals(clientId, metadata.getClientId());
        assertEquals("photo.jpg", metadata.getOriginalName());
        assertEquals("photo-a1b2c3d4.jpg", metadata.getSystemName());
        assertEquals("abc123", metadata.getChecksum());
        assertEquals(1024, metadata.getSizeBytes());
        assertEquals("/app1/images/photo-a1b2c3d4.jpg", metadata.getStoragePath());
    }
}
