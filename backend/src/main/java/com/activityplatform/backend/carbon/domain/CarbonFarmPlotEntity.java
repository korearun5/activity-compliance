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
@Table(name = "carbon_farm_plots")
public class CarbonFarmPlotEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "carbon_profile_id", nullable = false)
  private CarbonProfileEntity carbonProfile;

  @Column(name = "farm_name", nullable = false)
  private String farmName;

  @Column(name = "survey_number")
  private String surveyNumber;

  @Column(name = "area_acres", nullable = false)
  private BigDecimal areaAcres;

  @Column(nullable = false)
  private BigDecimal latitude;

  @Column(nullable = false)
  private BigDecimal longitude;

  @Column(name = "irrigation_source")
  private String irrigationSource;

  @Column(name = "primary_crop")
  private String primaryCrop;

  @Column(name = "tillage_status")
  private String tillageStatus;

  @Column(length = 255)
  private String variety;

  @Column(length = 255)
  private String rootstock;

  @Column(name = "planting_date")
  private LocalDate plantingDate;

  @Column(name = "block_code", length = 100)
  private String blockCode;

  @Column(length = 50)
  private String spacing;

  @Column(name = "row_count")
  private Integer rowCount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CarbonRecordStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected CarbonFarmPlotEntity() {
  }

  public CarbonFarmPlotEntity(
      UUID id,
      TenantEntity tenant,
      CarbonProfileEntity carbonProfile,
      String farmName,
      String surveyNumber,
      BigDecimal areaAcres,
      BigDecimal latitude,
      BigDecimal longitude,
      String irrigationSource,
      String primaryCrop,
      String tillageStatus,
      String variety,
      String rootstock,
      LocalDate plantingDate,
      String blockCode,
      String spacing,
      Integer rowCount,
      CarbonRecordStatus status,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.carbonProfile = carbonProfile;
    this.farmName = farmName;
    this.surveyNumber = surveyNumber;
    this.areaAcres = areaAcres;
    this.latitude = latitude;
    this.longitude = longitude;
    this.irrigationSource = irrigationSource;
    this.primaryCrop = primaryCrop;
    this.tillageStatus = tillageStatus;
    this.variety = variety;
    this.rootstock = rootstock;
    this.plantingDate = plantingDate;
    this.blockCode = blockCode;
    this.spacing = spacing;
    this.rowCount = rowCount;
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

  public String getFarmName() {
    return farmName;
  }

  public String getSurveyNumber() {
    return surveyNumber;
  }

  public BigDecimal getAreaAcres() {
    return areaAcres;
  }

  public BigDecimal getLatitude() {
    return latitude;
  }

  public BigDecimal getLongitude() {
    return longitude;
  }

  public String getIrrigationSource() {
    return irrigationSource;
  }

  public String getPrimaryCrop() {
    return primaryCrop;
  }

  public String getTillageStatus() {
    return tillageStatus;
  }

  public String getVariety() {
    return variety;
  }

  public String getRootstock() {
    return rootstock;
  }

  public LocalDate getPlantingDate() {
    return plantingDate;
  }

  public String getBlockCode() {
    return blockCode;
  }

  public String getSpacing() {
    return spacing;
  }

  public Integer getRowCount() {
    return rowCount;
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
      String farmName,
      String surveyNumber,
      BigDecimal areaAcres,
      BigDecimal latitude,
      BigDecimal longitude,
      String irrigationSource,
      String primaryCrop,
      String tillageStatus,
      String variety,
      String rootstock,
      LocalDate plantingDate,
      String blockCode,
      String spacing,
      Integer rowCount,
      CarbonRecordStatus status,
      Instant now
  ) {
    this.farmName = farmName;
    this.surveyNumber = surveyNumber;
    this.areaAcres = areaAcres;
    this.latitude = latitude;
    this.longitude = longitude;
    this.irrigationSource = irrigationSource;
    this.primaryCrop = primaryCrop;
    this.tillageStatus = tillageStatus;
    this.variety = variety;
    this.rootstock = rootstock;
    this.plantingDate = plantingDate;
    this.blockCode = blockCode;
    this.spacing = spacing;
    this.rowCount = rowCount;
    this.status = status;
    this.updatedAt = now;
  }
}
