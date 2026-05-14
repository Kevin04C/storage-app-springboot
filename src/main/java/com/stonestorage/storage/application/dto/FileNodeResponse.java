package com.stonestorage.storage.application.dto;

import com.stonestorage.storage.domain.entity.FileNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Nodo de archivo o carpeta listado")
public record FileNodeResponse(
        @Schema(description = "Nombre del nodo", example = "photo.jpg")
        String name,
        @Schema(description = "Tipo de nodo: FILE o FOLDER", example = "FILE")
        FileNode.NodeType type,
        @Schema(description = "Tamaño en bytes", example = "2048")
        long size,
        @Schema(description = "Fecha de última modificación")
        Instant lastModified,
        @Schema(description = "URL de miniatura si es imagen", example = "/api/v1/storage/thumbnail?path=photo.jpg&w=200&h=200")
        String thumbnailUrl
) {

    public static FileNodeResponse from(FileNode node, String baseUrl) {
        String thumb = null;
        if (isImage(node.getName())) {
            thumb = baseUrl + "/thumbnails?path=" + node.getName() + "&w=200&h=200";
        }
        return new FileNodeResponse(
                node.getName(),
                node.getType(),
                node.getSize(),
                node.getLastModified(),
                thumb
        );
    }

    private static boolean isImage(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".webp");
    }
}
