package com.stonestorage.shared.domain.port;

public interface PathSanitizerPort {
    String sanitize(String clientBaseDir, String relativePath);
    String toAbsolutePath(String relativePath);
}
