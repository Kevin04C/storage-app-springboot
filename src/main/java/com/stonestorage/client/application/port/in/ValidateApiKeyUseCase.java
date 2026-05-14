package com.stonestorage.client.application.port.in;

import com.stonestorage.client.domain.entity.Client;
import reactor.core.publisher.Mono;

public interface ValidateApiKeyUseCase {

    Mono<Client> validate(String apiKey);
}
