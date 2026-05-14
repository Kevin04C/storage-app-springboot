package com.stonestorage.shared.domain.exception;

public class PathTraversalException extends DomainException {

    public PathTraversalException(String path) {
        super("Path traversal detected in: " + path);
    }
}
