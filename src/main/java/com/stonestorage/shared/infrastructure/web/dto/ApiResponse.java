package com.stonestorage.shared.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
@Schema(description = "Formato estandarizado de respuesta de la API")
public class ApiResponse<T> {

    @Schema(description = "Indica si la operación fue exitosa", example = "true")
    private final boolean success;
    @Schema(description = "Mensaje descriptivo de la operación", example = "File uploaded successfully")
    private final String message;
    @Schema(description = "Payload de la respuesta")
    private final T data;
    @Schema(description = "Detalle del error si la operación falló")
    private final ApiError error;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Información de paginación (solo en endpoints paginados)")
    private final PaginationInfo pagination;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .error(null)
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .error(null)
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, String message, PaginationInfo pagination) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .error(null)
                .pagination(pagination)
                .build();
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String code, String details) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(status.name())
                .data(null)
                .error(ApiError.builder()
                        .code(code)
                        .details(details)
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String code, String details) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .error(ApiError.builder()
                        .code(code)
                        .details(details)
                        .build())
                .build();
    }
}
