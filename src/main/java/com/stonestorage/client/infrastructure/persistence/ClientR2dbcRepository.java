package com.stonestorage.client.infrastructure.persistence;

import com.stonestorage.client.domain.entity.Client;
import com.stonestorage.client.domain.repository.ClientRepository;
import com.stonestorage.client.infrastructure.persistence.entity.ClientEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClientR2dbcRepository implements ClientRepository {

    private final R2dbcEntityTemplate template;

    @Override
    public Mono<Client> findByApiKey(String apiKey) {
        return template.select(Query.query(Criteria.where("api_key").is(apiKey)), ClientEntity.class)
                .next()
                .map(this::toDomain);
    }

    @Override
    public Mono<Client> findById(UUID id) {
        return template.selectOne(Query.query(Criteria.where("id").is(id)), ClientEntity.class)
                .map(this::toDomain);
    }

    @Override
    public Mono<Client> save(Client client) {
        ClientEntity entity = toEntity(client);
        return template.insert(entity).map(this::toDomain);
    }

    @Override
    public Mono<Void> updateUsedBytes(UUID clientId, long usedBytes) {
        return template.update(
                Query.query(Criteria.where("id").is(clientId)),
                Update.update("used_bytes", usedBytes),
                ClientEntity.class
        ).then();
    }

    @Override
    public Mono<Boolean> existsByAppName(String appName) {
        return template.exists(Query.query(Criteria.where("app_name").is(appName)), ClientEntity.class);
    }

    private Client toDomain(ClientEntity entity) {
        return Client.builder()
                .id(entity.getId())
                .appName(entity.getAppName())
                .apiKey(entity.getApiKey())
                .baseDir(entity.getBaseDir())
                .quotaBytes(entity.getQuotaBytes())
                .usedBytes(entity.getUsedBytes())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ClientEntity toEntity(Client client) {
        ClientEntity entity = new ClientEntity();
        entity.setId(client.getId());
        entity.setAppName(client.getAppName());
        entity.setApiKey(client.getApiKey());
        entity.setBaseDir(client.getBaseDir());
        entity.setQuotaBytes(client.getQuotaBytes());
        entity.setUsedBytes(client.getUsedBytes());
        entity.setActive(client.isActive());
        entity.setCreatedAt(client.getCreatedAt());
        return entity;
    }
}
