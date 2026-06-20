package com.stonestorage.shared.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.api-key.ttl-minutes:10}")
    private int apiKeyTtlMinutes;

    @Value("${cache.api-key.max-size:1000}")
    private int apiKeyMaxSize;

    @Value("${cache.file-metadata.ttl-minutes:60}")
    private int fileMetadataTtlMinutes;

    @Value("${cache.file-metadata.max-size:5000}")
    private int fileMetadataMaxSize;

    @Value("${cache.thumbnails.ttl-minutes:60}")
    private int thumbnailsTtlMinutes;

    @Value("${cache.thumbnails.max-size:500}")
    private int thumbnailsMaxSize;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("apiKeys", "fileMetadata", "thumbnails");
        cacheManager.setAsyncCacheMode(true);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(apiKeyTtlMinutes))
                .maximumSize(apiKeyMaxSize));
        return cacheManager;
    }
}
