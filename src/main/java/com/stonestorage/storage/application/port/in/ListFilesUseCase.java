package com.stonestorage.storage.application.port.in;

import com.stonestorage.storage.application.dto.FileNodeResponse;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface ListFilesUseCase {

    Flux<FileNodeResponse> list(UUID clientId, String baseDir, String path);
}
