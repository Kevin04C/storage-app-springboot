package com.stonestorage.client.domain.exception;

import com.stonestorage.shared.domain.exception.DomainException;

public class ClientNotFoundException extends DomainException {

    public ClientNotFoundException(String apiKey) {
        super("Client not found or inactive for API key: " + apiKey);
    }
}
