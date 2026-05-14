package com.stonestorage.client.domain.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClientTest {

    @Test
    void hasEnoughQuota_withinLimit_returnsTrue() {
        Client client = Client.builder()
                .id(UUID.randomUUID())
                .quotaBytes(1000)
                .usedBytes(100)
                .build();

        assertTrue(client.hasEnoughQuota(500));
    }

    @Test
    void hasEnoughQuota_exceedsLimit_returnsFalse() {
        Client client = Client.builder()
                .id(UUID.randomUUID())
                .quotaBytes(1000)
                .usedBytes(900)
                .build();

        assertFalse(client.hasEnoughQuota(200));
    }

    @Test
    void addUsedBytes_shouldIncrease() {
        Client client = Client.builder()
                .id(UUID.randomUUID())
                .quotaBytes(1000)
                .usedBytes(100)
                .build();

        client.addUsedBytes(50);
        assertEquals(150, client.getUsedBytes());
    }

    @Test
    void subtractUsedBytes_shouldNotGoNegative() {
        Client client = Client.builder()
                .id(UUID.randomUUID())
                .quotaBytes(1000)
                .usedBytes(100)
                .build();

        client.subtractUsedBytes(200);
        assertEquals(0, client.getUsedBytes());
    }
}
