package com.stonestorage.client.application.port.in;

import com.stonestorage.client.application.dto.RegisterClientRequest;
import com.stonestorage.client.application.dto.ClientResponse;
import reactor.core.publisher.Mono;

public interface RegisterClientUseCase {

    Mono<ClientResponse> register(RegisterClientRequest request);
}
