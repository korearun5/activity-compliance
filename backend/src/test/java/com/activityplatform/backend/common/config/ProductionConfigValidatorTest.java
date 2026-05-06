package com.activityplatform.backend.common.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.activityplatform.backend.auth.config.AuthProperties;
import com.activityplatform.backend.security.CorsProperties;
import com.activityplatform.backend.storage.MinioStorageProperties;
import com.activityplatform.backend.storage.StorageProperties;
import com.activityplatform.backend.storage.StorageProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;

class ProductionConfigValidatorTest {
  @Test
  void testAcceptsSafeProductionConfig() {
    validator().validate();
  }

  @Test
  void testRejectsLocalDatabaseUrl() {
    ProductionConfigValidator validator = validator();
    validatorDataSource(validator).setUrl("jdbc:postgresql://localhost:5432/activity_platform");

    assertThatThrownBy(validator::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("APP_DB_URL must not point at localhost");
  }

  @Test
  void testRejectsUnsafeJwtSecret() {
    ProductionConfigValidator validator = validator();
    validatorAuth(validator).setSecret("local-development-secret-value-that-is-long-enough");

    assertThatThrownBy(validator::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("unsafe placeholder");
  }

  @Test
  void testRejectsHttpCorsOrigin() {
    ProductionConfigValidator validator = validator();
    validatorCors(validator).setAllowedOrigins(List.of("http://app.example.com"));

    assertThatThrownBy(validator::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("must use HTTPS");
  }

  @Test
  void testRejectsNonMinioStorageProvider() {
    ProductionConfigValidator validator = validator();
    validatorStorage(validator).setProvider(StorageProvider.LOCAL);

    assertThatThrownBy(validator::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("APP_STORAGE_PROVIDER must be minio");
  }

  @Test
  void testRejectsInsecureMinioEndpointWhenSecureModeIsEnabled() {
    ProductionConfigValidator validator = validator();
    validatorMinio(validator).setEndpoint("http://minio.example.com");
    validatorMinio(validator).setSecure(true);

    assertThatThrownBy(validator::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("APP_MINIO_ENDPOINT must use HTTPS");
  }

  private ProductionConfigValidator validator() {
    AuthProperties authProperties = new AuthProperties();
    authProperties.setSecret("prod-strong-value-1234567890-ABCDEFGHIJKLMNOPQRSTUVWXYZ");

    CorsProperties corsProperties = new CorsProperties();
    corsProperties.setAllowedOrigins(List.of("https://app.example.com"));

    DataSourceProperties dataSourceProperties = new DataSourceProperties();
    dataSourceProperties.setUrl("jdbc:postgresql://db.example.com:5432/activity_platform");
    dataSourceProperties.setUsername("activity_app");
    dataSourceProperties.setPassword("strong-password");

    StorageProperties storageProperties = new StorageProperties();
    storageProperties.setProvider(StorageProvider.MINIO);

    MinioStorageProperties minioStorageProperties = new MinioStorageProperties();
    minioStorageProperties.setEndpoint("https://minio.example.com");
    minioStorageProperties.setBucket("activity-platform");
    minioStorageProperties.setAccessKey("access-key");
    minioStorageProperties.setSecretKey("secret-key");

    return new TestableProductionConfigValidator(
        authProperties,
        corsProperties,
        dataSourceProperties,
        minioStorageProperties,
        storageProperties
    );
  }

  private AuthProperties validatorAuth(ProductionConfigValidator validator) {
    return ((TestableProductionConfigValidator) validator).authProperties;
  }

  private CorsProperties validatorCors(ProductionConfigValidator validator) {
    return ((TestableProductionConfigValidator) validator).corsProperties;
  }

  private DataSourceProperties validatorDataSource(ProductionConfigValidator validator) {
    return ((TestableProductionConfigValidator) validator).dataSourceProperties;
  }

  private StorageProperties validatorStorage(ProductionConfigValidator validator) {
    return ((TestableProductionConfigValidator) validator).storageProperties;
  }

  private MinioStorageProperties validatorMinio(ProductionConfigValidator validator) {
    return ((TestableProductionConfigValidator) validator).minioStorageProperties;
  }

  private static class TestableProductionConfigValidator extends ProductionConfigValidator {
    private final AuthProperties authProperties;
    private final CorsProperties corsProperties;
    private final DataSourceProperties dataSourceProperties;
    private final MinioStorageProperties minioStorageProperties;
    private final StorageProperties storageProperties;

    TestableProductionConfigValidator(
        AuthProperties authProperties,
        CorsProperties corsProperties,
        DataSourceProperties dataSourceProperties,
        MinioStorageProperties minioStorageProperties,
        StorageProperties storageProperties
    ) {
      super(
          authProperties,
          corsProperties,
          dataSourceProperties,
          minioStorageProperties,
          storageProperties
      );
      this.authProperties = authProperties;
      this.corsProperties = corsProperties;
      this.dataSourceProperties = dataSourceProperties;
      this.minioStorageProperties = minioStorageProperties;
      this.storageProperties = storageProperties;
    }
  }
}
