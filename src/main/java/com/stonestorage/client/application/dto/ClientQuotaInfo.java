package com.stonestorage.client.application.dto;

import com.stonestorage.shared.domain.valueobject.FriendlySize;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Información de cuota del cliente")
public record ClientQuotaInfo(
        @Schema(description = "Cuota total en bytes", example = "21474836480")
        long quotaBytes,
        @Schema(description = "Espacio usado en bytes", example = "0")
        long usedBytes,
        @Schema(description = "Espacio disponible en bytes", example = "21474836480")
        long availableBytes,
        @Schema(description = "Cuota total legible", example = "20 GB")
        String quotaFriendly,
        @Schema(description = "Espacio usado legible", example = "0 B")
        String usedFriendly,
        @Schema(description = "Espacio disponible legible", example = "20 GB")
        String availableFriendly
) {

    public static ClientQuotaInfo from(long quotaBytes, long usedBytes) {
        long available = Math.max(0, quotaBytes - usedBytes);
        return new ClientQuotaInfo(
                quotaBytes,
                usedBytes,
                available,
                FriendlySize.fromBytes(quotaBytes).value(),
                FriendlySize.fromBytes(usedBytes).value(),
                FriendlySize.fromBytes(available).value()
        );
    }
}
