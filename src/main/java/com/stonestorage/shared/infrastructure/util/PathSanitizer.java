package com.stonestorage.shared.infrastructure.util;

import com.stonestorage.shared.domain.exception.PathTraversalException;
import com.stonestorage.shared.domain.port.PathSanitizerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class PathSanitizer implements PathSanitizerPort {

    private static final Logger log = LoggerFactory.getLogger(PathSanitizer.class);
    private static final String[] DANGEROUS_SEQUENCES = {"..", "~", "%2e%2e", "%252e%252e"};

    private final Path storageBasePath;

    public PathSanitizer(@Value("${storage.base-path:./storage}") String basePath) {
        log.info("Initializing PathSanitizer with storage.base-path: {}", basePath);
        this.storageBasePath = Paths.get(basePath).toAbsolutePath().normalize();
        log.info("Resolved storage base path: {}", this.storageBasePath);
    }

    /**
     * Sanitiza y devuelve la ruta relativa (sin storage.base-path).
     * Esta es la ruta que se guarda en base de datos.
     * Ejemplo: /ClientFolder/imagenes/foto-abc123.jpg
     */
    public String sanitize(String clientBaseDir, String relativePath) {
        if (clientBaseDir == null || clientBaseDir.isBlank()) {
            throw new PathTraversalException("Base directory cannot be empty");
        }
        if (relativePath == null || relativePath.isBlank()) {
            relativePath = "/";
        }

        String normalized = relativePath.replace("\\", "/").trim();
        for (String seq : DANGEROUS_SEQUENCES) {
            if (normalized.contains(seq)) {
                throw new PathTraversalException(relativePath);
            }
        }

        // Construir ruta relativa: /clientBaseDir/relativePath
        String relPath = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        String result = "/" + clientBaseDir + (relPath.isEmpty() ? "" : "/" + relPath);
        
        // Validar que no escape del base path
        Path fullPath = storageBasePath.resolve(clientBaseDir).resolve(relPath).normalize();
        if (!fullPath.startsWith(storageBasePath)) {
            throw new PathTraversalException(relativePath);
        }

        return result.replace("\\", "/");
    }

    /**
     * Convierte una ruta relativa (de BD) a ruta absoluta (para acceso a disco).
     * Ejemplo: /ClientFolder/imagenes/foto.jpg -> D:/storage/ClientFolder/imagenes/foto.jpg
     */
    public String toAbsolutePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new PathTraversalException("Relative path cannot be empty");
        }
        String normalized = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        Path resolved = storageBasePath.resolve(normalized).normalize();
        
        if (!resolved.startsWith(storageBasePath)) {
            throw new PathTraversalException(relativePath);
        }
        
        return resolved.toString().replace("\\", "/");
    }

    public Path getStorageBasePath() {
        return storageBasePath;
    }
}
