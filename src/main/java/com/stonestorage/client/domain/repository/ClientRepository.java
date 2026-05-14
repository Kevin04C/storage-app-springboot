package com.stonestorage.client.domain.repository;

import com.stonestorage.client.domain.entity.Client;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ClientRepository {

    Mono<Client> findByApiKey(String apiKey);

    Mono<Client> findById(UUID id);

    Mono<Client> save(Client client);

    Mono<Void> updateUsedBytes(UUID clientId, long usedBytes);

    Mono<Boolean> existsByAppName(String appName);
}
