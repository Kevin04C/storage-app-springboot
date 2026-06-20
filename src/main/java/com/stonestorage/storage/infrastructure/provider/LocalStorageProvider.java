package com.stonestorage.storage.infrastructure.provider;

import com.stonestorage.storage.domain.entity.FileNode;
import com.stonestorage.storage.domain.port.StorageProvider;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.List;

public class LocalStorageProvider implements StorageProvider {

    @Override
    public Mono<Void> save(InputStream file, String fullPath) {
        return Mono.fromRunnable(() -> {
            try {
                Path path = Paths.get(fullPath);
                Files.createDirectories(path.getParent());
                Files.copy(file, path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save file to " + fullPath, e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Flux<DataBuffer> load(String fullPath) {
        return Flux.defer(() -> {
            Path path = Paths.get(fullPath);
            if (!Files.exists(path)) {
                return Flux.error(new RuntimeException("File not found: " + fullPath));
            }
            return DataBufferUtils.read(path, DefaultDataBufferFactory.sharedInstance, 65536);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<FileNode> listContents(String path) {
        return Mono.fromCallable(() -> {
            Path dir = Paths.get(path);
            if (!Files.exists(dir)) {
                return List.<FileNode>of();
            }
            try (var stream = Files.list(dir)) {
                return stream.map(p -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                        return FileNode.builder()
                                .name(p.getFileName().toString())
                                .type(attrs.isDirectory() ? FileNode.NodeType.FOLDER : FileNode.NodeType.FILE)
                                .size(attrs.size())
                                .lastModified(attrs.lastModifiedTime().toInstant())
                                .build();
                    } catch (IOException e) {
                        return FileNode.builder()
                                .name(p.getFileName().toString())
                                .type(FileNode.NodeType.FILE)
                                .size(0)
                                .lastModified(Instant.now())
                                .build();
                    }
                }).toList();
            }
        }).subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<Void> delete(String fullPath) {
        return Mono.fromRunnable(() -> {
            try {
                Path path = Paths.get(fullPath);
                if (Files.exists(path)) {
                    Files.delete(path);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete file " + fullPath, e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Long> getFolderSize(String path) {
        return Mono.fromCallable(() -> {
            Path dir = Paths.get(path);
            if (!Files.exists(dir)) {
                return 0L;
            }
            try (var stream = Files.walk(dir)) {
                return stream.filter(p -> !Files.isDirectory(p))
                        .mapToLong(p -> {
                            try {
                                return Files.size(p);
                            } catch (IOException e) {
                                return 0L;
                            }
                        }).sum();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
