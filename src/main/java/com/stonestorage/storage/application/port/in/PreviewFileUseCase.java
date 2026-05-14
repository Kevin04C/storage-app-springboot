package com.stonestorage.storage.application.port.in;

import com.stonestorage.storage.application.dto.FileContent;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PreviewFileUseCase {
    Mono<FileContent> preview(UUID fileId);
}
