package com.stonestorage.client.domain.service;

import com.stonestorage.client.domain.entity.Client;
import com.stonestorage.client.domain.exception.QuotaExceededException;
import org.springframework.stereotype.Service;

@Service
public class ClientDomainService {

    public void validateQuota(Client client, long requestedBytes) {
        if (!client.hasEnoughQuota(requestedBytes)) {
            throw new QuotaExceededException(
                    "Quota exceeded. Available: " + (client.getQuotaBytes() - client.getUsedBytes())
                            + ", Requested: " + requestedBytes);
        }
    }

    public void registerUsage(Client client, long bytes) {
        client.addUsedBytes(bytes);
    }

    public void releaseUsage(Client client, long bytes) {
        client.subtractUsedBytes(bytes);
    }
}
