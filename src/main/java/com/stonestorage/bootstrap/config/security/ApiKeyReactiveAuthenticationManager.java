package com.stonestorage.bootstrap.config.security;

import com.stonestorage.client.application.port.in.ValidateApiKeyUseCase;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ApiKeyReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final ValidateApiKeyUseCase validateApiKeyUseCase;

    public ApiKeyReactiveAuthenticationManager(ValidateApiKeyUseCase validateApiKeyUseCase) {
        this.validateApiKeyUseCase = validateApiKeyUseCase;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (authentication instanceof ApiKeyAuthenticationToken token) {
            String apiKey = (String) token.getCredentials();
            return validateApiKeyUseCase.validate(apiKey)
                    .map(client -> new ApiKeyAuthenticationToken(apiKey, client));
        }
        return Mono.empty();
    }
}
