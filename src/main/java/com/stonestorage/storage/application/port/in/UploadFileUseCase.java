package com.stonestorage.storage.application.port.in;

import com.stonestorage.storage.application.dto.UploadRequest;
import com.stonestorage.storage.application.dto.UploadResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UploadFileUseCase {

    Mono<UploadResponse> upload(UUID clientId, String baseDir, long quotaBytes, long usedBytes, UploadRequest request);
}
