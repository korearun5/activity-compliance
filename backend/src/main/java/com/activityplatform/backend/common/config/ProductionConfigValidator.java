package com.activityplatform.backend.common.config;

import com.activityplatform.backend.auth.config.AuthProperties;
import com.activityplatform.backend.security.CorsProperties;
import com.activityplatform.backend.storage.MinioStorageProperties;
import com.activityplatform.backend.storage.StorageProperties;
import com.activityplatform.backend.storage.StorageProvider;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionConfigValidator implements ApplicationRunner {
  private static final List<String> UNSAFE_SECRET_MARKERS = List.of(
      "change",
      "default",
      "dev",
      "local",
      "secret"
  );

  private final AuthProperties authProperties;
  private final CorsProperties corsProperties;
  private final DataSourceProperties dataSourceProperties;
  private final MinioStorageProperties minioStorageProperties;
  private final StorageProperties storageProperties;

  public ProductionConfigValidator(
      AuthProperties authProperties,
      CorsProperties corsProperties,
      DataSourceProperties dataSourceProperties,
      MinioStorageProperties minioStorageProperties,
      StorageProperties storageProperties
  ) {
    this.authProperties = authProperties;
    this.corsProperties = corsProperties;
    this.dataSourceProperties = dataSourceProperties;
    this.minioStorageProperties = minioStorageProperties;
    this.storageProperties = storageProperties;
  }

  @Override
  public void run(ApplicationArguments args) {
    validate();
  }

  public void validate() {
    validateDatabase();
    validateJwt();
    validateCors();
    validateStorage();
  }

  private void validateDatabase() {
    requireText(dataSourceProperties.getUrl(), "APP_DB_URL is required in production.");
    requireText(dataSourceProperties.getUsername(), "APP_DB_USERNAME is required in production.");
    requireText(dataSourceProperties.getPassword(), "APP_DB_PASSWORD is required in production.");

    String url = dataSourceProperties.getUrl().toLowerCase();
    if (url.contains("localhost") || url.contains("127.0.0.1")) {
      throw invalid("APP_DB_URL must not point at localhost in production.");
    }
  }

  private void validateJwt() {
    requireText(authProperties.getSecret(), "APP_JWT_SECRET is required in production.");
    if (authProperties.getSecret().length() < 48) {
      throw invalid("APP_JWT_SECRET must be at least 48 characters in production.");
    }

    String secret = authProperties.getSecret().toLowerCase();
    if (UNSAFE_SECRET_MARKERS.stream().anyMatch(secret::contains)) {
      throw invalid("APP_JWT_SECRET appears to contain an unsafe placeholder value.");
    }
  }

  private void validateCors() {
    if (corsProperties.getAllowedOrigins().isEmpty()) {
      throw invalid("APP_CORS_ALLOWED_ORIGINS is required in production.");
    }

    for (String origin : corsProperties.getAllowedOrigins()) {
      if ("*".equals(origin)) {
        throw invalid("Wildcard CORS origins are not allowed in production.");
      }
      if (!origin.startsWith("https://")) {
        throw invalid("Production CORS origins must use HTTPS.");
      }
    }
  }

  private void validateStorage() {
    if (storageProperties.getProvider() != StorageProvider.MINIO) {
      throw invalid("APP_STORAGE_PROVIDER must be minio in production.");
    }

    requireText(minioStorageProperties.getEndpoint(), "APP_MINIO_ENDPOINT is required in production.");
    requireText(minioStorageProperties.getBucket(), "APP_MINIO_BUCKET is required in production.");
    requireText(minioStorageProperties.getAccessKey(), "APP_MINIO_ACCESS_KEY is required in production.");
    requireText(minioStorageProperties.getSecretKey(), "APP_MINIO_SECRET_KEY is required in production.");

    String endpoint = minioStorageProperties.getEndpoint().trim().toLowerCase();
    if (minioStorageProperties.isSecure() && endpoint.startsWith("http://")) {
      throw invalid("APP_MINIO_ENDPOINT must use HTTPS when APP_MINIO_SECURE is true.");
    }
  }

  private void requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw invalid(message);
    }
  }

  private IllegalStateException invalid(String message) {
    return new IllegalStateException("Production configuration error: " + message);
  }
}
