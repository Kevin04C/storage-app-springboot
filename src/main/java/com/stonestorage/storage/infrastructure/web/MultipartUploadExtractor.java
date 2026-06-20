package com.stonestorage.storage.infrastructure.web;

import com.stonestorage.storage.application.dto.UploadRequest;
import com.stonestorage.storage.domain.FileVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.http.codec.multipart.Part;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class MultipartUploadExtractor {

    public Mono<UploadRequest> extract(MultiValueMap<String, Part> parts) {
        return Mono.defer(() -> {
            var filePart = parts.getFirst("file");
            if (!(filePart instanceof FilePart fp)) {
                return Mono.error(new IllegalArgumentException("El campo 'file' es requerido"));
            }

            String path = extractPath(parts);
            FileVisibility visibility = extractVisibility(parts);
            long sizeBytes = extractSizeBytes(fp);

            return Mono.just(new UploadRequest(
                    fp.filename(),
                    path,
                    fp.content(),
                    sizeBytes,
                    visibility
            ));
        });
    }

    private String extractPath(MultiValueMap<String, Part> parts) {
        var pathPart = parts.getFirst("path");
        if (pathPart instanceof FormFieldPart pathField && !pathField.value().isBlank()) {
            String path = pathField.value().trim();
            return path.startsWith("/") ? path : "/" + path;
        }
        return "/";
    }

    private FileVisibility extractVisibility(MultiValueMap<String, Part> parts) {
        var visPart = parts.getFirst("visibility");
        if (visPart instanceof FormFieldPart visField && !visField.value().isBlank()) {
            try {
                return FileVisibility.valueOf(visField.value().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return FileVisibility.PUBLIC;
    }

    private long extractSizeBytes(FilePart filePart) {
        var headers = filePart.headers();
        var contentLength = headers.getContentLength();
        return contentLength >= 0 ? contentLength : 0;
    }
}
