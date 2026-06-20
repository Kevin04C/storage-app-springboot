package com.stonestorage.client.infrastructure.persistence;

import com.stonestorage.client.domain.repository.ClientRepository;
import com.stonestorage.shared.domain.port.ClientQuotaPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClientQuotaAdapter implements ClientQuotaPort {

    private final ClientRepository clientRepository;

    @Override
    public Mono<Void> updateUsedBytes(UUID clientId, long usedBytes) {
        return clientRepository.updateUsedBytes(clientId, usedBytes);
    }
}
