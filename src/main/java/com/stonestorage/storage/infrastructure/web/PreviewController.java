package com.stonestorage.storage.infrastructure.web;

import com.stonestorage.storage.application.port.in.PreviewFileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;

import java.time.Duration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLConnection;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Vista Pública", description = "Acceso público a archivos sin API Key")
public class PreviewController {

    private final PreviewFileUseCase previewFileUseCase;

    @GetMapping("/f/{fileId}")
    @Operation(
            summary = "Previsualizar archivo público",
            description = "Descarga un archivo público sin necesidad de API Key. " +
                    "Si el archivo es PRIVATE, retorna 403 Forbidden."
    )
    @ApiResponse(responseCode = "200", description = "Archivo servido correctamente")
    @ApiResponse(responseCode = "403", description = "El archivo es privado")
    @ApiResponse(responseCode = "404", description = "Archivo no encontrado")
    public Mono<Void> preview(
            @Parameter(description = "ID del archivo", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID fileId,
            ServerWebExchange exchange) {
        return previewFileUseCase.preview(fileId)
                .flatMap(fileContent -> {
                    String fileName = fileContent.getFileName();
                    String contentType = URLConnection.guessContentTypeFromName(fileName);
                    if (contentType == null || contentType.isBlank()) {
                        contentType = "application/octet-stream";
                    }

                    exchange.getResponse().getHeaders().setContentType(MediaType.parseMediaType(contentType));
                    exchange.getResponse().getHeaders().setContentDisposition(
                            ContentDisposition.builder("inline").filename(fileName).build()
                    );
                    exchange.getResponse().getHeaders().setContentLength(fileContent.getSizeBytes());
                    exchange.getResponse().getHeaders().setCacheControl(
                            CacheControl.maxAge(Duration.ofDays(30)).cachePublic()
                    );

                    return exchange.getResponse().writeWith(fileContent.getContent());
                });
    }
}
