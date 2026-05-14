package com.stonestorage.storage.application.service;

import com.stonestorage.client.domain.exception.QuotaExceededException;
import com.stonestorage.client.domain.repository.ClientRepository;
import com.stonestorage.client.domain.service.ClientDomainService;
import com.stonestorage.shared.infrastructure.util.PathSanitizer;
import com.stonestorage.storage.application.dto.FileContent;
import com.stonestorage.storage.application.dto.FileNodeResponse;
import com.stonestorage.storage.application.dto.UploadRequest;
import com.stonestorage.storage.application.dto.UploadResponse;
import com.stonestorage.storage.application.port.in.*;
import com.stonestorage.storage.domain.entity.FileMetadata;
import com.stonestorage.storage.domain.exception.FileNotFoundException;
import com.stonestorage.storage.domain.exception.StorageException;
import com.stonestorage.storage.domain.port.StorageProvider;
import com.stonestorage.storage.domain.repository.FileMetadataRepository;
import com.stonestorage.storage.domain.service.StorageDomainService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorageApplicationService implements UploadFileUseCase, DownloadFileUseCase, ListFilesUseCase, DeleteFileUseCase, GenerateThumbnailUseCase, PreviewFileUseCase {

    private final FileMetadataRepository fileMetadataRepository;
    private final StorageDomainService storageDomainService;
    private final ClientRepository clientRepository;
    private final PathSanitizer pathSanitizer;
    private final StorageProvider storageProvider;

    @Override
    @Transactional
    public Mono<UploadResponse> upload(UUID clientId, String baseDir, long quotaBytes, long usedBytes, UploadRequest request) {
        return DataBufferUtils.join(request.content())
                .publishOn(Schedulers.boundedElastic())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    if (request.sizeBytes() > 0 && bytes.length != request.sizeBytes()) {
                        return Mono.error(new StorageException("Content size mismatch"));
                    }

                    long finalSize = request.sizeBytes() > 0 ? request.sizeBytes() : bytes.length;
                    if (usedBytes + finalSize > quotaBytes) {
                        return Mono.error(new QuotaExceededException("Quota exceeded. Available: " + (quotaBytes - usedBytes)));
                    }

                    String checksum = sha256(bytes);

                    String path = request.path();
                    String systemName = generateSystemName(request.originalName());
                    String filePath = (path.endsWith("/") ? path : path + "/") + systemName;
                    
                    // Ruta relativa para BD (sin storage.base-path)
                    String storagePath = pathSanitizer.sanitize(baseDir, filePath);
                    // Ruta absoluta para guardar en disco
                    String absolutePath = pathSanitizer.toAbsolutePath(storagePath);

                    FileMetadata metadata = storageDomainService.createMetadata(
                            clientId, request.originalName(), systemName, checksum, finalSize, request.visibility(), storagePath);

                    long finalUsedBytes = usedBytes + finalSize;

                    return storageProvider.save(new ByteArrayInputStream(bytes), absolutePath)
                            .then(fileMetadataRepository.save(metadata))
                            .flatMap(saved -> clientRepository
                                    .updateUsedBytes(clientId, finalUsedBytes)
                                    .thenReturn(saved)
                            )
                            .map(saved -> new UploadResponse(
                                    saved.getId(),
                                    saved.getOriginalName(),
                                    saved.getSystemName(),
                                    saved.getStoragePath(),
                                    saved.getChecksum(),
                                    saved.getSizeBytes(),
                                    saved.getVisibility(),
                                    "/f/" + saved.getId(),
                                    "/api/v1/storage/download/" + saved.getId()
                            ));
                });
    }

    @Override
    public Flux<DataBuffer> download(UUID clientId, UUID fileId) {
        return fileMetadataRepository.findById(fileId)
                .filter(fm -> fm.getClientId().equals(clientId) && !fm.isDeleted())
                .switchIfEmpty(Mono.error(new FileNotFoundException("File not found: " + fileId)))
                .flatMapMany(fm -> storageProvider.load(pathSanitizer.toAbsolutePath(fm.getStoragePath()))
                            .flatMapMany(is -> {
                                try {
                                    byte[] bytes = IOUtils.toByteArray(is);
                                    return Flux.just(org.springframework.core.io.buffer.DefaultDataBufferFactory.sharedInstance.wrap(bytes));
                                } catch (IOException e) {
                                    return Flux.error(new StorageException("Failed to read file", e));
                                }
                            })
                );
    }

    @Override
    public Mono<FileContent> preview(UUID fileId) {
        return fileMetadataRepository.findById(fileId)
                .filter(fm -> !fm.isDeleted())
                .filter(FileMetadata::isPublic)
                .switchIfEmpty(Mono.error(new FileNotFoundException("File not found or not public: " + fileId)))
                .flatMap(fm -> storageProvider.load(pathSanitizer.toAbsolutePath(fm.getStoragePath()))
                        .map(is -> {
                            try {
                                byte[] bytes = IOUtils.toByteArray(is);
                                Flux<DataBuffer> content = Flux.just(
                                        org.springframework.core.io.buffer.DefaultDataBufferFactory.sharedInstance.wrap(bytes)
                                );
                                return FileContent.builder()
                                        .fileName(fm.getOriginalName())
                                        .sizeBytes(fm.getSizeBytes())
                                        .content(content)
                                        .build();
                            } catch (IOException e) {
                                throw new StorageException("Failed to read file", e);
                            }
                        })
                );
    }

    @Override
    public Flux<FileNodeResponse> list(UUID clientId, String baseDir, String path) {
        String relativePath = pathSanitizer.sanitize(baseDir, path);
        String absolutePath = pathSanitizer.toAbsolutePath(relativePath);
        return storageProvider.listContents(absolutePath)
                .map(node -> FileNodeResponse.from(node, "/api/v1/storage"));
    }

    @Override
    @Transactional
    public Mono<Void> delete(UUID clientId, UUID fileId) {
        return fileMetadataRepository.findById(fileId)
                .filter(fm -> fm.getClientId().equals(clientId) && !fm.isDeleted())
                .switchIfEmpty(Mono.error(new FileNotFoundException("File not found: " + fileId)))
                .flatMap(fm -> fileMetadataRepository.softDelete(fileId));
    }

    @Override
    public Mono<byte[]> generate(String fullPath, int width, int height) {
        return storageProvider.load(fullPath)
                .publishOn(Schedulers.boundedElastic())
                .flatMap(inputStream -> {
                    try {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        net.coobird.thumbnailator.Thumbnails.of(inputStream)
                                .size(width, height)
                                .toOutputStream(os);
                        return Mono.just(os.toByteArray());
                    } catch (IOException e) {
                        return Mono.error(new StorageException("Thumbnail generation failed", e));
                    }
                });
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new StorageException("SHA-256 not available", e);
        }
    }

    private String generateSystemName(String originalName) {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        int lastDot = originalName.lastIndexOf('.');
        if (lastDot > 0) {
            String name = originalName.substring(0, lastDot);
            String extension = originalName.substring(lastDot);
            return name + "-" + uniqueSuffix + extension;
        }
        return originalName + "-" + uniqueSuffix;
    }
}
