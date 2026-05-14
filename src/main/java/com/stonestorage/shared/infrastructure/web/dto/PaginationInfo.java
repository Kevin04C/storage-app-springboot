package com.stonestorage.shared.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Información de paginación (opcional)")
public class PaginationInfo {

    @Schema(description = "Página actual", example = "0")
    private final int page;
    @Schema(description = "Tamaño de página", example = "20")
    private final int size;
    @Schema(description = "Total de elementos", example = "100")
    private final long totalElements;
    @Schema(description = "Total de páginas", example = "5")
    private final int totalPages;

    public static PaginationInfo empty() {
        return PaginationInfo.builder()
                .page(0)
                .size(0)
                .totalElements(0)
                .totalPages(0)
                .build();
    }

    public static PaginationInfo of(int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return PaginationInfo.builder()
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }
}
