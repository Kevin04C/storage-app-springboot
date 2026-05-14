package com.stonestorage.client.application.service;

import com.stonestorage.client.application.dto.ClientQuotaInfo;
import com.stonestorage.client.application.dto.ClientResponse;
import com.stonestorage.client.application.dto.RegisterClientRequest;
import com.stonestorage.client.application.port.in.GetClientQuotaUseCase;
import com.stonestorage.client.application.port.in.RegisterClientUseCase;
import com.stonestorage.client.application.port.in.ValidateApiKeyUseCase;
import com.stonestorage.client.domain.entity.Client;
import com.stonestorage.client.domain.exception.ClientAlreadyExistsException;
import com.stonestorage.client.domain.exception.ClientNotFoundException;
import com.stonestorage.client.domain.repository.ClientRepository;
import com.stonestorage.client.domain.service.ClientDomainService;
import com.stonestorage.shared.domain.valueobject.FriendlySize;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientApplicationService implements ValidateApiKeyUseCase, GetClientQuotaUseCase, RegisterClientUseCase {

    private final ClientRepository clientRepository;
    private final ClientDomainService clientDomainService;

    @Override
    @Cacheable(value = "apiKeys", key = "#apiKey")
    public Mono<Client> validate(String apiKey) {
        return clientRepository.findByApiKey(apiKey)
                .filter(Client::isActive)
                .switchIfEmpty(Mono.error(new ClientNotFoundException(apiKey)));
    }

    @Override
    @Cacheable(value = "quotas", key = "#clientId")
    public Mono<ClientQuotaInfo> getQuota(UUID clientId) {
        return clientRepository.findById(clientId)
                .map(client -> ClientQuotaInfo.from(client.getQuotaBytes(), client.getUsedBytes()));
    }

    @Override
    public Mono<ClientResponse> register(RegisterClientRequest request) {
        return clientRepository.existsByAppName(request.appName())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.<ClientResponse>error(new ClientAlreadyExistsException(request.appName()));
                    }

                    long quotaBytes = request.quotaGb() != null
                            ? FriendlySize.toBytes(request.quotaGb() + "GB")
                            : FriendlySize.toBytes("20GB");

                    String baseDir = toPascalCase(request.appName()) + "_folder";
                    String apiKey = UUID.randomUUID().toString();

                    Client client = Client.builder()
                            .id(UUID.randomUUID())
                            .appName(request.appName())
                            .apiKey(apiKey)
                            .baseDir(baseDir)
                            .quotaBytes(quotaBytes)
                            .usedBytes(0)
                            .active(true)
                            .createdAt(Instant.now())
                            .build();

                    return clientRepository.save(client)
                            .map(saved -> ClientResponse.from(
                                    saved.getId(),
                                    saved.getAppName(),
                                    saved.getApiKey(),
                                    saved.getBaseDir(),
                                    saved.getQuotaBytes(),
                                    saved.getUsedBytes(),
                                    saved.isActive(),
                                    saved.getCreatedAt()
                            ));
                });
    }

    private String toPascalCase(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String[] words = input.split("[\\s_\\-]+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }
        return result.toString();
    }
}
