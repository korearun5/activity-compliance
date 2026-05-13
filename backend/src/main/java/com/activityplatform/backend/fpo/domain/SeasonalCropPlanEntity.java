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

  @Column(name = "planned_sowing_date")
  private LocalDate plannedSowingDate;

  @Column(name = "expected_harvest_date")
  private LocalDate expectedHarvestDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CropPlanStatus status;

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
      BigDecimal plannedAreaAcres,
      LocalDate plannedSowingDate,
      LocalDate expectedHarvestDate,
      CropPlanStatus status,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.memberProfile = memberProfile;
    this.plot = plot;
    this.crop = crop;
    this.season = season;
    this.plannedAreaAcres = plannedAreaAcres;
    this.plannedSowingDate = plannedSowingDate;
    this.expectedHarvestDate = expectedHarvestDate;
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

  public LocalDate getPlannedSowingDate() {
    return plannedSowingDate;
  }

  public LocalDate getExpectedHarvestDate() {
    return expectedHarvestDate;
  }

  public CropPlanStatus getStatus() {
    return status;
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
      BigDecimal plannedAreaAcres,
      LocalDate plannedSowingDate,
      LocalDate expectedHarvestDate,
      CropPlanStatus status,
      Instant now
  ) {
    this.memberProfile = memberProfile;
    this.plot = plot;
    this.crop = crop;
    this.season = season;
    this.plannedAreaAcres = plannedAreaAcres;
    this.plannedSowingDate = plannedSowingDate;
    this.expectedHarvestDate = expectedHarvestDate;
    this.status = status;
    this.updatedAt = now;
  }

  public void updateStatus(CropPlanStatus status, Instant now) {
    this.status = status;
    this.updatedAt = now;
  }
}
