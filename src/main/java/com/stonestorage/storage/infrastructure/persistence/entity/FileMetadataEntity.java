package com.stonestorage.storage.infrastructure.persistence.entity;

import com.stonestorage.storage.domain.FileVisibility;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Table("file_metadata")
public class FileMetadataEntity {

    @Id
    private UUID id;
    @Column("client_id")
    private UUID clientId;
    @Column("original_name")
    private String originalName;
    @Column("system_name")
    private String systemName;
    private String checksum;
    @Column("size_bytes")
    private long sizeBytes;
    private FileVisibility visibility;
    @Column("storage_path")
    private String storagePath;
    @Column("created_at")
    private Instant createdAt;
    @Column("deleted_at")
    private Instant deletedAt;
}
