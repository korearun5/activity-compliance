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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "seasonal_crop_plans")
public class SeasonalCropPlanEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_profile_id", nullable = false)
  private FpoMemberProfileEntity memberProfile;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plot_id")
  private FarmPlotEntity plot;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "crop_id", nullable = false)
  private CropCatalogEntity crop;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "season_id", nullable = false)
  private CropSeasonEntity season;

  @Column(name = "planned_area_acres", nullable = false)
  private BigDecimal plannedAreaAcres;

  @Column(name = "crop_year", nullable = false, length = 20)
  private String cropYear;

  @Column(name = "planned_sowing_date")
  private LocalDate plannedSowingDate;

  @Column(name = "expected_harvest_date")
  private LocalDate expectedHarvestDate;

  @Column(name = "expected_yield_quintals")
  private BigDecimal expectedYieldQuintals;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CropPlanStatus status;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected SeasonalCropPlanEntity() {
  }

  public SeasonalCropPlanEntity(
      UUID id,
      TenantEntity tenant,
      FpoMemberProfileEntity memberProfile,
      FarmPlotEntity plot,
      CropCatalogEntity crop,
      CropSeasonEntity season,
      String cropYear,
      BigDecimal plannedAreaAcres,
      LocalDate plannedSowingDate,
      LocalDate expectedHarvestDate,
      BigDecimal expectedYieldQuintals,
      CropPlanStatus status,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.memberProfile = memberProfile;
    this.plot = plot;
    this.crop = crop;
    this.season = season;
    this.cropYear = cropYear;
    this.plannedAreaAcres = plannedAreaAcres;
    this.plannedSowingDate = plannedSowingDate;
    this.expectedHarvestDate = expectedHarvestDate;
    this.createdAt = now;
    this.expectedYieldQuintals = expectedYieldQuintals;
    applyStatus(status, now);
  }

  public UUID getId() {
    return id;
  }

  public TenantEntity getTenant() {
    return tenant;
  }

  public FpoMemberProfileEntity getMemberProfile() {
    return memberProfile;
  }

  public FarmPlotEntity getPlot() {
    return plot;
  }

  public CropCatalogEntity getCrop() {
    return crop;
  }

  public CropSeasonEntity getSeason() {
    return season;
  }

  public BigDecimal getPlannedAreaAcres() {
    return plannedAreaAcres;
  }

  public String getCropYear() {
    return cropYear;
  }

  public LocalDate getPlannedSowingDate() {
    return plannedSowingDate;
  }

  public LocalDate getExpectedHarvestDate() {
    return expectedHarvestDate;
  }

  public BigDecimal getExpectedYieldQuintals() {
    return expectedYieldQuintals;
  }

  public CropPlanStatus getStatus() {
    return status;
  }

  public Instant getConfirmedAt() {
    return confirmedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updateDetails(
      FpoMemberProfileEntity memberProfile,
      FarmPlotEntity plot,
      CropCatalogEntity crop,
      CropSeasonEntity season,
      String cropYear,
      BigDecimal plannedAreaAcres,
      LocalDate plannedSowingDate,
      LocalDate expectedHarvestDate,
      BigDecimal expectedYieldQuintals,
      CropPlanStatus status,
      Instant now
  ) {
    this.memberProfile = memberProfile;
    this.plot = plot;
    this.crop = crop;
    this.season = season;
    this.cropYear = cropYear;
    this.plannedAreaAcres = plannedAreaAcres;
    this.plannedSowingDate = plannedSowingDate;
    this.expectedHarvestDate = expectedHarvestDate;
    this.expectedYieldQuintals = expectedYieldQuintals;
    applyStatus(status, now);
  }

  public void updateStatus(CropPlanStatus status, Instant now) {
    applyStatus(status, now);
  }

  private void applyStatus(CropPlanStatus status, Instant now) {
    if (status == CropPlanStatus.CONFIRMED && this.status != CropPlanStatus.CONFIRMED) {
      this.confirmedAt = now;
    }
    this.status = status;
    this.updatedAt = now;
  }
}
