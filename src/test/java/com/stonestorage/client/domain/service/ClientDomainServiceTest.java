package com.stonestorage.client.domain.service;

import com.stonestorage.client.domain.entity.Client;
import com.stonestorage.client.domain.exception.QuotaExceededException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClientDomainServiceTest {

    private final ClientDomainService service = new ClientDomainService();

    @Test
    void validateQuota_sufficientSpace_doesNotThrow() {
        Client client = Client.builder()
                .id(UUID.randomUUID())
                .quotaBytes(1000)
                .usedBytes(100)
                .build();

        assertDoesNotThrow(() -> service.validateQuota(client, 500));
    }

    @Test
    void validateQuota_exceededSpace_throwsException() {
        Client client = Client.builder()
                .id(UUID.randomUUID())
                .quotaBytes(1000)
                .usedBytes(900)
                .build();

        assertThrows(QuotaExceededException.class, () -> service.validateQuota(client, 200));
    }

    @Test
    void registerUsage_shouldIncreaseUsedBytes() {
        Client client = Client.builder()
                .id(UUID.randomUUID())
                .quotaBytes(1000)
                .usedBytes(100)
                .build();

        service.registerUsage(client, 100);
        assertEquals(200, client.getUsedBytes());
    }

    @Test
    void releaseUsage_shouldDecreaseUsedBytes() {
        Client client = Client.builder()
                .id(UUID.randomUUID())
                .quotaBytes(1000)
                .usedBytes(500)
                .build();

        service.releaseUsage(client, 100);
        assertEquals(400, client.getUsedBytes());
    }
}
