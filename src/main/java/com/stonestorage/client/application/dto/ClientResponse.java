package com.stonestorage.client.application.dto;

import com.stonestorage.shared.domain.valueobject.FriendlySize;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Respuesta con datos del cliente registrado")
public record ClientResponse(
        @Schema(description = "ID del cliente", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(description = "Nombre de la aplicación", example = "my_news_app")
        String appName,
        @Schema(description = "API Key generada para el cliente", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String apiKey,
        @Schema(description = "Carpeta base asignada", example = "MyNewsApp_folder")
        String baseDir,
        @Schema(description = "Cuota total legible", example = "20 GB")
        String quota,
        @Schema(description = "Espacio usado legible", example = "0 B")
        String used,
        @Schema(description = "Indica si el cliente está activo", example = "true")
        boolean active,
        @Schema(description = "Fecha de registro")
        Instant createdAt
) {

    public static ClientResponse from(UUID id, String appName, String apiKey, String baseDir,
                                       long quotaBytes, long usedBytes, boolean active, Instant createdAt) {
        return new ClientResponse(
                id,
                appName,
                apiKey,
                baseDir,
                FriendlySize.fromBytes(quotaBytes).value(),
                FriendlySize.fromBytes(usedBytes).value(),
                active,
                createdAt
        );
    }
}
