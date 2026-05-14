package com.stonestorage.shared.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FriendlySizeTest {

    @Test
    void fromBytes_shouldFormatBytes() {
        assertEquals("512 B", FriendlySize.fromBytes(512).value());
    }

    @Test
    void fromBytes_shouldFormatKB() {
        assertTrue(FriendlySize.fromBytes(1536).value().contains("KB"));
    }

    @Test
    void fromBytes_shouldFormatMB() {
        assertTrue(FriendlySize.fromBytes(2L * 1024 * 1024).value().contains("MB"));
    }

    @Test
    void fromBytes_shouldFormatGB() {
        assertTrue(FriendlySize.fromBytes(3L * 1024 * 1024 * 1024).value().contains("GB"));
    }

    @Test
    void toBytes_shouldParseKB() {
        assertEquals(2048, FriendlySize.toBytes("2KB"));
    }

    @Test
    void toBytes_shouldParseMB() {
        assertEquals(2L * 1024 * 1024, FriendlySize.toBytes("2 MB"));
    }

    @Test
    void toBytes_shouldParseGB() {
        assertEquals(1073741824L, FriendlySize.toBytes("1GB"));
    }

    @Test
    void toBytes_shouldThrowOnInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> FriendlySize.toBytes("invalid"));
    }
}
