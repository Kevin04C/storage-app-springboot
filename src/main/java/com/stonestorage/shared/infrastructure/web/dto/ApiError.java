package com.stonestorage.shared.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Detalle de un error en la respuesta")
public class ApiError {

    @Schema(description = "Código del error", example = "UNAUTHORIZED")
    private final String code;
    @Schema(description = "Descripción del error", example = "Client not found or inactive for API key: xyz")
    private final String details;
}
