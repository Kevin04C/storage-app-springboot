package com.stonestorage.storage.domain.exception;

import com.stonestorage.shared.domain.exception.DomainException;

public class StorageException extends DomainException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
