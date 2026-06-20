package com.stonestorage.storage.infrastructure.persistence;

import com.stonestorage.storage.domain.FileVisibility;
import com.stonestorage.storage.domain.entity.FileMetadata;
import com.stonestorage.storage.domain.repository.FileMetadataRepository;
import com.stonestorage.storage.infrastructure.persistence.entity.FileMetadataEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FileMetadataR2dbcRepository implements FileMetadataRepository {

    private final R2dbcEntityTemplate template;

    @Override
    @Cacheable(value = "fileMetadata", key = "#id")
    public Mono<FileMetadata> findById(UUID id) {
        return template.selectOne(Query.query(Criteria.where("id").is(id)), FileMetadataEntity.class)
                .map(this::toDomain);
    }

    @Override
    public Mono<FileMetadata> save(FileMetadata fileMetadata) {
        FileMetadataEntity entity = toEntity(fileMetadata);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        return template.insert(entity).map(this::toDomain);
    }

    @Override
    @CacheEvict(value = "fileMetadata", key = "#id")
    public Mono<Void> softDelete(UUID id) {
        return template.update(
                Query.query(Criteria.where("id").is(id)),
                Update.update("deleted_at", Instant.now()),
                FileMetadataEntity.class
        ).then();
    }

    @Override
    public Mono<Void> hardDelete(UUID id) {
        return template.delete(Query.query(Criteria.where("id").is(id)), FileMetadataEntity.class)
                .then();
    }

    @Override
    public Mono<Long> sumSizeByClientId(UUID clientId) {
        String sql = "SELECT COALESCE(SUM(size_bytes), 0) FROM file_metadata WHERE client_id = $1 AND deleted_at IS NULL";
        return template.getDatabaseClient()
                .sql(sql)
                .bind("$1", clientId)
                .map(row -> row.get(0, Long.class))
                .first();
    }

    private FileMetadata toDomain(FileMetadataEntity entity) {
        return FileMetadata.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .originalName(entity.getOriginalName())
                .systemName(entity.getSystemName())
                .checksum(entity.getChecksum())
                .sizeBytes(entity.getSizeBytes())
                .visibility(entity.getVisibility())
                .storagePath(entity.getStoragePath())
                .createdAt(entity.getCreatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private FileMetadataEntity toEntity(FileMetadata domain) {
        FileMetadataEntity entity = new FileMetadataEntity();
        entity.setId(domain.getId());
        entity.setClientId(domain.getClientId());
        entity.setOriginalName(domain.getOriginalName());
        entity.setSystemName(domain.getSystemName());
        entity.setChecksum(domain.getChecksum());
        entity.setSizeBytes(domain.getSizeBytes());
        entity.setVisibility(domain.getVisibility());
        entity.setStoragePath(domain.getStoragePath());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setDeletedAt(domain.getDeletedAt());
        return entity;
    }
}
