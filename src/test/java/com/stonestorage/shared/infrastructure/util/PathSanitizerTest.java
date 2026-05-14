package com.stonestorage.shared.infrastructure.util;

import com.stonestorage.shared.domain.exception.PathTraversalException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PathSanitizerTest {

    private final PathSanitizer sanitizer = new PathSanitizer("./storage");

    @Test
    void sanitize_validPath_shouldReturnRelativePath() {
        String result = sanitizer.sanitize("app1", "/docs/file.txt");
        assertEquals("/app1/docs/file.txt", result);
    }

    @Test
    void sanitize_pathTraversal_shouldThrow() {
        assertThrows(PathTraversalException.class, () -> sanitizer.sanitize("app1", "../../../etc/passwd"));
    }

    @Test
    void sanitize_nullBaseDir_shouldThrow() {
        assertThrows(PathTraversalException.class, () -> sanitizer.sanitize(null, "/file.txt"));
    }

    @Test
    void toAbsolutePath_shouldCombineWithBasePath() {
        String result = sanitizer.toAbsolutePath("/app1/docs/file.txt");
        assertTrue(result.endsWith("storage/app1/docs/file.txt"));
    }

    @Test
    void toAbsolutePath_pathTraversal_shouldThrow() {
        assertThrows(PathTraversalException.class, () -> sanitizer.toAbsolutePath("/../../../etc/passwd"));
    }
}
