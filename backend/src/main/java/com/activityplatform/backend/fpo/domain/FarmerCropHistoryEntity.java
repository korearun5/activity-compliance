package com.activityplatform.backend.fpo.domain;

import com.activityplatform.backend.auth.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "farmer_crop_history")
public class FarmerCropHistoryEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_profile_id", nullable = false)
  private FpoMemberProfileEntity memberProfile;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "crop_id", nullable = false)
  private CropCatalogEntity crop;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "season_id")
  private CropSeasonEntity season;

  @Column(name = "crop_year")
  private Integer cropYear;

  @Column(name = "area_acres")
  private BigDecimal areaAcres;

  @Column(name = "yield_quantity")
  private BigDecimal yieldQuantity;

  @Column(name = "yield_unit")
  private String yieldUnit;

  private String notes;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected FarmerCropHistoryEntity() {
  }

  public FarmerCropHistoryEntity(
      UUID id,
      TenantEntity tenant,
      FpoMemberProfileEntity memberProfile,
      CropCatalogEntity crop,
      CropSeasonEntity season,
      Integer cropYear,
      BigDecimal areaAcres,
      BigDecimal yieldQuantity,
      String yieldUnit,
      String notes,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.memberProfile = memberProfile;
    this.crop = crop;
    this.season = season;
    this.cropYear = cropYear;
    this.areaAcres = areaAcres;
    this.yieldQuantity = yieldQuantity;
    this.yieldUnit = yieldUnit;
    this.notes = notes;
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

  public CropCatalogEntity getCrop() {
    return crop;
  }

  public CropSeasonEntity getSeason() {
    return season;
  }

  public Integer getCropYear() {
    return cropYear;
  }

  public BigDecimal getAreaAcres() {
    return areaAcres;
  }

  public BigDecimal getYieldQuantity() {
    return yieldQuantity;
  }

  public String getYieldUnit() {
    return yieldUnit;
  }

  public String getNotes() {
    return notes;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updateDetails(
      CropCatalogEntity crop,
      CropSeasonEntity season,
      Integer cropYear,
      BigDecimal areaAcres,
      BigDecimal yieldQuantity,
      String yieldUnit,
      String notes,
      Instant now
  ) {
    this.crop = crop;
    this.season = season;
    this.cropYear = cropYear;
    this.areaAcres = areaAcres;
    this.yieldQuantity = yieldQuantity;
    this.yieldUnit = yieldUnit;
    this.notes = notes;
    this.updatedAt = now;
  }
}
