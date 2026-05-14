package com.stonestorage.client.infrastructure.persistence.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Table("storage_clients")
public class ClientEntity {

    @Id
    private UUID id;
    @Column("app_name")
    private String appName;
    @Column("api_key")
    private String apiKey;
    @Column("base_dir")
    private String baseDir;
    @Column("quota_bytes")
    private long quotaBytes;
    @Column("used_bytes")
    private long usedBytes;
    @Column("is_active")
    private boolean active;
    @Column("created_at")
    private Instant createdAt;
}
