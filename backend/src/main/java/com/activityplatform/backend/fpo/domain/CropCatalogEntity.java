package com.activityplatform.backend.fpo.domain;

import com.activityplatform.backend.auth.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crop_catalog")
public class CropCatalogEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @Column(name = "crop_code", nullable = false)
  private String code;

  @Column(name = "crop_name", nullable = false)
  private String name;

  private String category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FarmRecordStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected CropCatalogEntity() {
  }

  public CropCatalogEntity(
      UUID id,
      TenantEntity tenant,
      String code,
      String name,
      String category,
      FarmRecordStatus status,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.code = code;
    this.name = name;
    this.category = category;
    this.status = status;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public TenantEntity getTenant() {
    return tenant;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }

  public FarmRecordStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updateDetails(
      String code,
      String name,
      String category,
      FarmRecordStatus status,
      Instant now
  ) {
    this.code = code;
    this.name = name;
    this.category = category;
    this.status = status;
    this.updatedAt = now;
  }

  public void updateStatus(FarmRecordStatus status, Instant now) {
    this.status = status;
    this.updatedAt = now;
  }
}
