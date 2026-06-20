package com.stonestorage.shared.domain.port;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ClientQuotaPort {
    Mono<Void> updateUsedBytes(UUID clientId, long usedBytes);
}
