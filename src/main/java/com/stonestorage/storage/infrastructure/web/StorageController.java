package com.stonestorage.storage.infrastructure.web;

import com.stonestorage.shared.infrastructure.web.dto.ApiResponse;
import com.stonestorage.storage.application.dto.FileNodeResponse;
import com.stonestorage.storage.application.dto.UploadResponse;
import com.stonestorage.storage.application.port.in.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Tag(name = "Almacenamiento", description = "Operaciones de gestión de archivos")
public class StorageController {

    private final UploadFileUseCase uploadFileUseCase;
    private final DownloadFileUseCase downloadFileUseCase;
    private final ListFilesUseCase listFilesUseCase;
    private final DeleteFileUseCase deleteFileUseCase;
    private final GenerateThumbnailUseCase generateThumbnailUseCase;
    private final MultipartUploadExtractor uploadExtractor;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir archivo", description = "Sube un archivo verificando cuota e integridad. Requiere multipart: file (requerido), path (opcional, default /), visibility (opcional, default PUBLIC).")
    @SecurityRequirement(name = "ApiKeyAuth")
    public Mono<ApiResponse<UploadResponse>> upload(
            @RequestAttribute("clientId") UUID clientId,
            @RequestAttribute("clientBaseDir") String baseDir,
            @RequestAttribute("clientQuotaBytes") long quotaBytes,
            @RequestAttribute("clientUsedBytes") long usedBytes,
            ServerWebExchange exchange) {

        return exchange.getMultipartData()
                .flatMap(uploadExtractor::extract)
                .flatMap(request -> uploadFileUseCase.upload(clientId, baseDir, quotaBytes, usedBytes, request))
                .map(uploaded -> ApiResponse.ok(uploaded, "File uploaded successfully"));
    }

    @GetMapping("/download/{fileId}")
    @Operation(summary = "Descargar archivo", description = "Descarga un archivo por su ID.")
    @SecurityRequirement(name = "ApiKeyAuth")
    public Flux<DataBuffer> download(
            @RequestAttribute("clientId") UUID clientId,
            @Parameter(description = "ID del archivo", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID fileId) {
        return downloadFileUseCase.download(clientId, fileId);
    }

    @GetMapping("/list")
    @Operation(summary = "Listar archivos", description = "Lista archivos y carpetas en una ruta relativa del cliente.")
    @SecurityRequirement(name = "ApiKeyAuth")
    public Mono<ApiResponse<List<FileNodeResponse>>> list(
            @RequestAttribute("clientId") UUID clientId,
            @RequestAttribute("clientBaseDir") String baseDir,
            @Parameter(description = "Ruta relativa dentro del cliente", example = "/")
            @RequestParam(defaultValue = "/") String path) {
        return listFilesUseCase.list(clientId, baseDir, path)
                .collectList()
                .map(items -> ApiResponse.ok(items, "Files listed successfully"));
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Eliminar archivo", description = "Elimina un archivo (soft delete) por su ID.")
    @SecurityRequirement(name = "ApiKeyAuth")
    public Mono<ApiResponse<Void>> delete(
            @RequestAttribute("clientId") UUID clientId,
            @Parameter(description = "ID del archivo", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID fileId) {
        return deleteFileUseCase.delete(clientId, fileId)
                .then(Mono.just(ApiResponse.ok(null, "File deleted successfully")));
    }

    @GetMapping(value = "/thumbnail", produces = MediaType.IMAGE_JPEG_VALUE)
    @Operation(summary = "Generar miniatura", description = "Genera una miniatura JPEG de una imagen con el ancho y alto indicados.")
    @SecurityRequirement(name = "ApiKeyAuth")
    public Mono<ResponseEntity<byte[]>> thumbnail(
            @RequestAttribute("clientBaseDir") String baseDir,
            @Parameter(description = "Ruta del archivo", example = "/images/photo.jpg")
            @RequestParam String path,
            @Parameter(description = "Ancho", example = "200")
            @RequestParam(defaultValue = "200") int w,
            @Parameter(description = "Alto", example = "200")
            @RequestParam(defaultValue = "200") int h) {
        return generateThumbnailUseCase.generate(path, w, h)
                .map(bytes -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(bytes));
    }
}
