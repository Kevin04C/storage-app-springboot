package com.stonestorage.storage.application.dto;

import com.stonestorage.storage.domain.FileVisibility;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Respuesta de carga de archivo")
public record UploadResponse(
        @Schema(description = "ID del archivo", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(description = "Nombre original", example = "photo.jpg")
        String originalName,
        @Schema(description = "Nombre generado por el sistema", example = "photo-a1b2c3d4.jpg")
        String systemName,
        @Schema(description = "Ruta de almacenamiento relativa", example = "/ClientFolder/images/photo-a1b2c3d4.jpg")
        String storagePath,
        @Schema(description = "Checksum SHA-256", example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        String checksum,
        @Schema(description = "Tamaño en bytes", example = "2048")
        long sizeBytes,
        @Schema(description = "Visibilidad", example = "PUBLIC")
        FileVisibility visibility,
        @Schema(description = "URL pública de previsualización (sin API Key)", example = "/f/550e8400-e29b-41d4-a716-446655440000")
        String publicUrl,
        @Schema(description = "URL privada de descarga (requiere API Key)", example = "/api/v1/storage/download/550e8400-e29b-41d4-a716-446655440000")
        String privateUrl
) {}
