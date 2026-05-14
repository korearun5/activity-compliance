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
@Table(name = "input_demand_estimates")
public class InputDemandEstimateEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "crop_plan_id", nullable = false)
  private SeasonalCropPlanEntity cropPlan;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "input_id", nullable = false)
  private InputCatalogEntity input;

  @Column(name = "estimated_quantity", nullable = false)
  private BigDecimal estimatedQuantity;

  @Column(name = "recommended_quantity_per_acre", nullable = false)
  private BigDecimal recommendedQuantityPerAcre;

  @Column(name = "total_demand_quantity", nullable = false)
  private BigDecimal totalDemandQuantity;

  @Column(name = "buffer_percent", nullable = false)
  private BigDecimal bufferPercent;

  @Column(name = "buffer_quantity", nullable = false)
  private BigDecimal bufferQuantity;

  @Column(name = "final_demand_quantity", nullable = false)
  private BigDecimal finalDemandQuantity;

  @Column(nullable = false)
  private String unit;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InputDemandEstimateStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected InputDemandEstimateEntity() {
  }

  public InputDemandEstimateEntity(
      UUID id,
      TenantEntity tenant,
      SeasonalCropPlanEntity cropPlan,
      InputCatalogEntity input,
      BigDecimal estimatedQuantity,
      BigDecimal recommendedQuantityPerAcre,
      BigDecimal totalDemandQuantity,
      BigDecimal bufferPercent,
      BigDecimal bufferQuantity,
      BigDecimal finalDemandQuantity,
      String unit,
      InputDemandEstimateStatus status,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.cropPlan = cropPlan;
    this.input = input;
    this.estimatedQuantity = estimatedQuantity;
    this.recommendedQuantityPerAcre = recommendedQuantityPerAcre;
    this.totalDemandQuantity = totalDemandQuantity;
    this.bufferPercent = bufferPercent;
    this.bufferQuantity = bufferQuantity;
    this.finalDemandQuantity = finalDemandQuantity;
    this.unit = unit;
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

  public SeasonalCropPlanEntity getCropPlan() {
    return cropPlan;
  }

  public InputCatalogEntity getInput() {
    return input;
  }

  public BigDecimal getEstimatedQuantity() {
    return estimatedQuantity;
  }

  public BigDecimal getRecommendedQuantityPerAcre() {
    return recommendedQuantityPerAcre;
  }

  public BigDecimal getTotalDemandQuantity() {
    return totalDemandQuantity;
  }

  public BigDecimal getBufferPercent() {
    return bufferPercent;
  }

  public BigDecimal getBufferQuantity() {
    return bufferQuantity;
  }

  public BigDecimal getFinalDemandQuantity() {
    return finalDemandQuantity;
  }

  public String getUnit() {
    return unit;
  }

  public InputDemandEstimateStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updateEstimate(
      BigDecimal estimatedQuantity,
      BigDecimal recommendedQuantityPerAcre,
      BigDecimal totalDemandQuantity,
      BigDecimal bufferPercent,
      BigDecimal bufferQuantity,
      BigDecimal finalDemandQuantity,
      String unit,
      InputDemandEstimateStatus status,
      Instant now
  ) {
    this.estimatedQuantity = estimatedQuantity;
    this.recommendedQuantityPerAcre = recommendedQuantityPerAcre;
    this.totalDemandQuantity = totalDemandQuantity;
    this.bufferPercent = bufferPercent;
    this.bufferQuantity = bufferQuantity;
    this.finalDemandQuantity = finalDemandQuantity;
    this.unit = unit;
    this.status = status;
    this.updatedAt = now;
  }
}
