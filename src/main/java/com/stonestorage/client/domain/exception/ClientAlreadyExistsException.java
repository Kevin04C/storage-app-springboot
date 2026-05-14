package com.stonestorage.client.domain.exception;

import com.stonestorage.shared.domain.exception.DomainException;

public class ClientAlreadyExistsException extends DomainException {

    public ClientAlreadyExistsException(String appName) {
        super("Client with app name already exists: " + appName);
    }
}
