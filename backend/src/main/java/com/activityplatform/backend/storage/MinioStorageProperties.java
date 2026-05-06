package com.activityplatform.backend.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.minio")
public class MinioStorageProperties {
  private String endpoint = "";
  private String bucket = "";
  private String accessKey = "";
  private String secretKey = "";
  private String region = "";
  private boolean secure = true;
  private boolean createBucketIfMissing = false;

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public boolean isSecure() {
    return secure;
  }

  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  public boolean isCreateBucketIfMissing() {
    return createBucketIfMissing;
  }

  public void setCreateBucketIfMissing(boolean createBucketIfMissing) {
    this.createBucketIfMissing = createBucketIfMissing;
  }
}
