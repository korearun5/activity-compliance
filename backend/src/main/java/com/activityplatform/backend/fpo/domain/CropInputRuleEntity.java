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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crop_input_rules")
public class CropInputRuleEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "crop_id", nullable = false)
  private CropCatalogEntity crop;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "input_id", nullable = false)
  private InputCatalogEntity input;

  @Column(name = "quantity_per_acre", nullable = false)
  private BigDecimal quantityPerAcre;

  @Column(name = "application_stage")
  private String applicationStage;

  private String notes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FarmRecordStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected CropInputRuleEntity() {
  }

  public CropInputRuleEntity(
      UUID id,
      TenantEntity tenant,
      CropCatalogEntity crop,
      InputCatalogEntity input,
      BigDecimal quantityPerAcre,
      String applicationStage,
      String notes,
      FarmRecordStatus status,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.crop = crop;
    this.input = input;
    this.quantityPerAcre = quantityPerAcre;
    this.applicationStage = applicationStage;
    this.notes = notes;
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

  public CropCatalogEntity getCrop() {
    return crop;
  }

  public InputCatalogEntity getInput() {
    return input;
  }

  public BigDecimal getQuantityPerAcre() {
    return quantityPerAcre;
  }

  public String getApplicationStage() {
    return applicationStage;
  }

  public String getNotes() {
    return notes;
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
      CropCatalogEntity crop,
      InputCatalogEntity input,
      BigDecimal quantityPerAcre,
      String applicationStage,
      String notes,
      FarmRecordStatus status,
      Instant now
  ) {
    this.crop = crop;
    this.input = input;
    this.quantityPerAcre = quantityPerAcre;
    this.applicationStage = applicationStage;
    this.notes = notes;
    this.status = status;
    this.updatedAt = now;
  }

  public void updateStatus(FarmRecordStatus status, Instant now) {
    this.status = status;
    this.updatedAt = now;
  }
}
