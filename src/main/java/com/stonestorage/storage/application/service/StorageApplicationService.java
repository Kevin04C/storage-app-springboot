package com.stonestorage.storage.application.service;

import com.stonestorage.client.domain.exception.QuotaExceededException;
import com.stonestorage.shared.domain.port.ClientQuotaPort;
import com.stonestorage.shared.domain.port.PathSanitizerPort;
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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorageApplicationService implements UploadFileUseCase, DownloadFileUseCase, ListFilesUseCase, DeleteFileUseCase, GenerateThumbnailUseCase, PreviewFileUseCase {

    private final FileMetadataRepository fileMetadataRepository;
    private final ClientQuotaPort clientQuotaPort;
    private final PathSanitizerPort pathSanitizer;
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

                    if (usedBytes + bytes.length > quotaBytes) {
                        return Mono.error(new QuotaExceededException("Quota exceeded. Available: " + (quotaBytes - usedBytes)));
                    }

                    String checksum = sha256(bytes);

                    String path = request.path();
                    String systemName = generateSystemName(request.originalName());
                    String filePath = (path.endsWith("/") ? path : path + "/") + systemName;

                    String storagePath = pathSanitizer.sanitize(baseDir, filePath);
                    String absolutePath = pathSanitizer.toAbsolutePath(storagePath);

                    FileMetadata metadata = FileMetadata.create(
                            clientId, request.originalName(), systemName, checksum, bytes.length, request.visibility(), storagePath);

                    long finalUsedBytes = usedBytes + bytes.length;

                    return storageProvider.save(new ByteArrayInputStream(bytes), absolutePath)
                            .then(fileMetadataRepository.save(metadata))
                            .flatMap(saved -> clientQuotaPort
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
                .flatMapMany(fm -> storageProvider.load(pathSanitizer.toAbsolutePath(fm.getStoragePath())));
    }

    @Override
    public Mono<FileContent> preview(UUID fileId) {
        return fileMetadataRepository.findById(fileId)
                .filter(fm -> !fm.isDeleted())
                .filter(FileMetadata::isPublic)
                .switchIfEmpty(Mono.error(new FileNotFoundException("File not found or not public: " + fileId)))
                .flatMap(fm -> {
                    Flux<DataBuffer> content = storageProvider.load(pathSanitizer.toAbsolutePath(fm.getStoragePath()));
                    return Mono.just(FileContent.builder()
                            .fileName(fm.getOriginalName())
                            .sizeBytes(fm.getSizeBytes())
                            .content(content)
                            .build());
                });
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
    @Cacheable(value = "thumbnails", key = "#fullPath + ':' + #width + 'x' + #height")
    public Mono<byte[]> generate(String fullPath, int width, int height) {
        return DataBufferUtils.join(storageProvider.load(fullPath))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    try {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        net.coobird.thumbnailator.Thumbnails.of(new ByteArrayInputStream(bytes))
                                .size(width, height)
                                .toOutputStream(os);
                        return Mono.just(os.toByteArray());
                    } catch (Exception e) {
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
