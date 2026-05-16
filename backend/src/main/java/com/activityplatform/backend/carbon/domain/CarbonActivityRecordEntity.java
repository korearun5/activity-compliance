package com.activityplatform.backend.carbon.domain;

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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "carbon_activity_records")
public class CarbonActivityRecordEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "carbon_profile_id", nullable = false)
  private CarbonProfileEntity carbonProfile;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "carbon_farm_plot_id")
  private CarbonFarmPlotEntity carbonFarmPlot;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private CarbonActivityCategoryEntity category;

  @Column(name = "activity_date", nullable = false)
  private LocalDate activityDate;

  @Column(name = "crop_name", nullable = false)
  private String cropName;

  @Column(name = "input_used")
  private String inputUsed;

  @Column(name = "quantity_value")
  private BigDecimal quantityValue;

  @Column(name = "quantity_unit")
  private String quantityUnit;

  private String remarks;

  @Column(name = "evidence_count", nullable = false)
  private int evidenceCount;

  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status", nullable = false)
  private CarbonActivityVerificationStatus verificationStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CarbonRecordStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected CarbonActivityRecordEntity() {
  }

  public CarbonActivityRecordEntity(
      UUID id,
      TenantEntity tenant,
      CarbonProfileEntity carbonProfile,
      CarbonFarmPlotEntity carbonFarmPlot,
      CarbonActivityCategoryEntity category,
      LocalDate activityDate,
      String cropName,
      String inputUsed,
      BigDecimal quantityValue,
      String quantityUnit,
      String remarks,
      CarbonRecordStatus status,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.carbonProfile = carbonProfile;
    this.carbonFarmPlot = carbonFarmPlot;
    this.category = category;
    this.activityDate = activityDate;
    this.cropName = cropName;
    this.inputUsed = inputUsed;
    this.quantityValue = quantityValue;
    this.quantityUnit = quantityUnit;
    this.remarks = remarks;
    this.evidenceCount = 0;
    this.verificationStatus = CarbonActivityVerificationStatus.PENDING_EVIDENCE;
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

  public CarbonProfileEntity getCarbonProfile() {
    return carbonProfile;
  }

  public CarbonFarmPlotEntity getCarbonFarmPlot() {
    return carbonFarmPlot;
  }

  public CarbonActivityCategoryEntity getCategory() {
    return category;
  }

  public LocalDate getActivityDate() {
    return activityDate;
  }

  public String getCropName() {
    return cropName;
  }

  public String getInputUsed() {
    return inputUsed;
  }

  public BigDecimal getQuantityValue() {
    return quantityValue;
  }

  public String getQuantityUnit() {
    return quantityUnit;
  }

  public String getRemarks() {
    return remarks;
  }

  public int getEvidenceCount() {
    return evidenceCount;
  }

  public CarbonActivityVerificationStatus getVerificationStatus() {
    return verificationStatus;
  }

  public CarbonRecordStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updateDetails(
      CarbonFarmPlotEntity carbonFarmPlot,
      CarbonActivityCategoryEntity category,
      LocalDate activityDate,
      String cropName,
      String inputUsed,
      BigDecimal quantityValue,
      String quantityUnit,
      String remarks,
      CarbonRecordStatus status,
      Instant now
  ) {
    this.carbonFarmPlot = carbonFarmPlot;
    this.category = category;
    this.activityDate = activityDate;
    this.cropName = cropName;
    this.inputUsed = inputUsed;
    this.quantityValue = quantityValue;
    this.quantityUnit = quantityUnit;
    this.remarks = remarks;
    this.status = status;
    this.updatedAt = now;
  }
}
