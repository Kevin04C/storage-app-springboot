package com.stonestorage.client.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request para registrar un nuevo cliente")
public record RegisterClientRequest(
        @Schema(description = "Nombre de la aplicación", example = "my_news_app")
        @NotBlank String appName,
        @Schema(description = "Cuota en GB (opcional, default 20)", example = "20")
        @Positive Double quotaGb
) {}
