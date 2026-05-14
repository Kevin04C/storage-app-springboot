package com.stonestorage.storage.infrastructure.provider;

import com.stonestorage.storage.domain.port.StorageProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageProviderFactory {

    @Value("${storage.type:local}")
    private String storageType;

    @Bean
    public StorageProvider storageProvider() {
        return switch (storageType.toLowerCase()) {
            case "s3" -> new S3StorageProvider();
            default -> new LocalStorageProvider();
        };
    }
}
