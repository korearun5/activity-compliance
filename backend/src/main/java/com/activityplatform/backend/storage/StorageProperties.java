package com.activityplatform.backend.storage;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
  private StorageProvider provider = StorageProvider.LOCAL;

  @Min(1)
  private long maxUploadBytes = 10 * 1024 * 1024;

  public StorageProvider getProvider() {
    return provider;
  }

  public void setProvider(StorageProvider provider) {
    this.provider = provider;
  }

  public long getMaxUploadBytes() {
    return maxUploadBytes;
  }

  public void setMaxUploadBytes(long maxUploadBytes) {
    this.maxUploadBytes = maxUploadBytes;
  }
}
