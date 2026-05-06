package com.activityplatform.backend.storage;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "minio")
public class MinioClientConfig {
  @Bean
  MinioClient minioClient(MinioStorageProperties properties) {
    validate(properties);

    MinioClient.Builder builder = MinioClient.builder()
        .endpoint(endpoint(properties))
        .credentials(properties.getAccessKey().trim(), properties.getSecretKey().trim());

    if (hasText(properties.getRegion())) {
      builder.region(properties.getRegion().trim());
    }

    return builder.build();
  }

  private String endpoint(MinioStorageProperties properties) {
    String endpoint = properties.getEndpoint().trim();
    if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
      return endpoint;
    }

    return (properties.isSecure() ? "https://" : "http://") + endpoint;
  }

  private void validate(MinioStorageProperties properties) {
    if (!hasText(properties.getEndpoint())) {
      throw new IllegalStateException("MinIO endpoint is required when app.storage.provider=minio.");
    }
    if (!hasText(properties.getBucket())) {
      throw new IllegalStateException("MinIO bucket is required when app.storage.provider=minio.");
    }
    if (!hasText(properties.getAccessKey())) {
      throw new IllegalStateException("MinIO access key is required when app.storage.provider=minio.");
    }
    if (!hasText(properties.getSecretKey())) {
      throw new IllegalStateException("MinIO secret key is required when app.storage.provider=minio.");
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
