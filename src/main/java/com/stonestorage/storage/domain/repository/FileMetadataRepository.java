package com.stonestorage.storage.domain.repository;

import com.stonestorage.storage.domain.entity.FileMetadata;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface FileMetadataRepository {

    Mono<FileMetadata> findById(UUID id);

    Mono<FileMetadata> save(FileMetadata fileMetadata);

    Mono<Void> softDelete(UUID id);

    Mono<Void> hardDelete(UUID id);

    Mono<Long> sumSizeByClientId(UUID clientId);
}
