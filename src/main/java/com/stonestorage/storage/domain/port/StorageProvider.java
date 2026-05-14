package com.stonestorage.storage.domain.port;

import com.stonestorage.storage.domain.entity.FileNode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.List;

public interface StorageProvider {

    Mono<Void> save(InputStream file, String fullPath);

    Mono<InputStream> load(String fullPath);

    Flux<FileNode> listContents(String path);

    Mono<Void> delete(String fullPath);

    Mono<Long> getFolderSize(String path);
}
