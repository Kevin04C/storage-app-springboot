CREATE TABLE IF NOT EXISTS storage_clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_name VARCHAR(255) NOT NULL,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    base_dir VARCHAR(500) NOT NULL,
    quota_bytes BIGINT NOT NULL DEFAULT 0,
    used_bytes BIGINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS file_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES storage_clients(id),
    original_name VARCHAR(500) NOT NULL,
    system_name VARCHAR(500) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    size_bytes BIGINT NOT NULL DEFAULT 0,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    storage_path VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_file_metadata_client ON file_metadata(client_id);
CREATE INDEX IF NOT EXISTS idx_file_metadata_storage_path ON file_metadata(storage_path);
CREATE INDEX IF NOT EXISTS idx_file_metadata_deleted ON file_metadata(deleted_at) WHERE deleted_at IS NULL;
