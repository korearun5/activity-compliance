package com.activityplatform.backend.fpo.domain;

import com.activityplatform.backend.auth.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fpo_advisory_images")
public class FpoAdvisoryImageEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "advisory_id", nullable = false)
  private FpoAdvisoryEntity advisory;

  @Column(name = "image_url", nullable = false)
  private String imageUrl;

  @Column(name = "storage_key")
  private String storageKey;

  @Column(name = "original_filename")
  private String originalFilename;

  @Column(name = "content_type")
  private String contentType;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected FpoAdvisoryImageEntity() {
  }

  public FpoAdvisoryImageEntity(
      UUID id,
      TenantEntity tenant,
      String imageUrl,
      String storageKey,
      String originalFilename,
      String contentType,
      Integer sortOrder,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.imageUrl = imageUrl;
    this.storageKey = storageKey;
    this.originalFilename = originalFilename;
    this.contentType = contentType;
    this.sortOrder = sortOrder;
    this.createdAt = now;
  }

  public UUID getId() {
    return id;
  }

  public TenantEntity getTenant() {
    return tenant;
  }

  public FpoAdvisoryEntity getAdvisory() {
    return advisory;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getOriginalFilename() {
    return originalFilename;
  }

  public String getContentType() {
    return contentType;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  void attachTo(FpoAdvisoryEntity advisory) {
    this.advisory = advisory;
  }
}
