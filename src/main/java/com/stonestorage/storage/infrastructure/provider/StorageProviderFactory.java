package com.stonestorage.storage.infrastructure.provider;

import com.stonestorage.storage.domain.port.StorageProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class StorageProviderFactory {

    @Value("${storage.type:local}")
    private String storageType;

    @PostConstruct
    void validate() {
        String type = storageType.toLowerCase();
        if ("s3".equals(type)) {
            log.error("S3 storage provider is selected but NOT YET IMPLEMENTED. " +
                    "Falling back to LocalStorageProvider to avoid runtime failures.");
            throw new IllegalStateException(
                    "S3 storage provider is selected but not implemented. " +
                    "Set storage.type=local or implement S3StorageProvider.");
        }
        log.info("Storage provider configured: {}", type);
    }

    @Bean
    public StorageProvider storageProvider() {
        String type = storageType.toLowerCase();
        if ("s3".equals(type)) {
            log.warn("S3 not implemented, falling back to local storage");
            return new LocalStorageProvider();
        }
        return new LocalStorageProvider();
    }
}
