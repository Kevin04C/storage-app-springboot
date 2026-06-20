package com.stonestorage.storage.infrastructure.provider;

import com.stonestorage.storage.domain.entity.FileNode;
import com.stonestorage.storage.domain.port.StorageProvider;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;

public class S3StorageProvider implements StorageProvider {

    @Override
    public Mono<Void> save(InputStream file, String fullPath) {
        return Mono.error(new UnsupportedOperationException("S3 provider not yet implemented"));
    }

    @Override
    public Flux<DataBuffer> load(String fullPath) {
        return Flux.error(new UnsupportedOperationException("S3 provider not yet implemented"));
    }

    @Override
    public Flux<FileNode> listContents(String path) {
        return Flux.error(new UnsupportedOperationException("S3 provider not yet implemented"));
    }

    @Override
    public Mono<Void> delete(String fullPath) {
        return Mono.error(new UnsupportedOperationException("S3 provider not yet implemented"));
    }

    @Override
    public Mono<Long> getFolderSize(String path) {
        return Mono.error(new UnsupportedOperationException("S3 provider not yet implemented"));
    }
}
