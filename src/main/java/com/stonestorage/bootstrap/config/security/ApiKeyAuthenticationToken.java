package com.stonestorage.bootstrap.config.security;

import com.stonestorage.client.domain.entity.Client;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String apiKey;
    private final Client client;

    public ApiKeyAuthenticationToken(String apiKey, Client client) {
        super(Collections.emptyList());
        this.apiKey = apiKey;
        this.client = client;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return client;
    }

    public Client getClient() {
        return client;
    }
}
