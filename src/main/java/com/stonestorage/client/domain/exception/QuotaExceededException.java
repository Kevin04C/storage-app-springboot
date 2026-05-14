package com.stonestorage.client.domain.exception;

import com.stonestorage.shared.domain.exception.DomainException;

public class QuotaExceededException extends DomainException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
