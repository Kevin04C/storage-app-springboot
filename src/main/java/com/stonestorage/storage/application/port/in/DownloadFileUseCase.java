package com.stonestorage.storage.application.port.in;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface DownloadFileUseCase {

    Flux<DataBuffer> download(UUID clientId, UUID fileId);
}
