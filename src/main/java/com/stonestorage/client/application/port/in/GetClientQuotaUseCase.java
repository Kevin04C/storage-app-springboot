package com.stonestorage.client.application.port.in;

import com.stonestorage.client.application.dto.ClientQuotaInfo;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface GetClientQuotaUseCase {

    Mono<ClientQuotaInfo> getQuota(UUID clientId);
}
