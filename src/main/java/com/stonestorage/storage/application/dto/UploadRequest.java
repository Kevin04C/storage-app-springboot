package com.stonestorage.storage.application.dto;

import com.stonestorage.storage.domain.FileVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

public record UploadRequest(
        @NotBlank String originalName,
        @NotBlank String path,
        @NotNull Flux<DataBuffer> content,
        long sizeBytes,
        FileVisibility visibility
) {}
