package com.stonestorage.storage.application.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

@Getter
@Builder
public class FileContent {
    private final String fileName;
    private final long sizeBytes;
    private final Flux<DataBuffer> content;
}
