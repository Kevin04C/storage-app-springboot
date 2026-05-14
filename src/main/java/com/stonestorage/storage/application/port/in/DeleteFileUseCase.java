package com.stonestorage.storage.application.port.in;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface DeleteFileUseCase {

    Mono<Void> delete(UUID clientId, UUID fileId);
}
