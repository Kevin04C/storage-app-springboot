package com.stonestorage.storage.application.port.in;

import reactor.core.publisher.Mono;

public interface GenerateThumbnailUseCase {

    Mono<byte[]> generate(String fullPath, int width, int height);
}
