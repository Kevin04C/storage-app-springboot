package com.stonestorage.storage.domain.exception;

import com.stonestorage.shared.domain.exception.DomainException;

public class FileNotFoundException extends DomainException {

    public FileNotFoundException(String message) {
        super(message);
    }
}
