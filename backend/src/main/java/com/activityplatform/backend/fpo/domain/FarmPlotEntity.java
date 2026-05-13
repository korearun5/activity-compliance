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
@Table(name = "farm_plots")
public class FarmPlotEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_profile_id", nullable = false)
  private FpoMemberProfileEntity memberProfile;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "landholding_id")
  private FarmLandholdingEntity landholding;

  @Column(name = "plot_name", nullable = false)
  private String plotName;

  @Column(name = "area_acres", nullable = false)
  private BigDecimal areaAcres;

  @Column(nullable = false)
  private BigDecimal latitude;

  @Column(nullable = false)
  private BigDecimal longitude;

  @Column(name = "soil_type")
  private String soilType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FarmRecordStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected FarmPlotEntity() {
  }

  public FarmPlotEntity(
      UUID id,
      TenantEntity tenant,
      FpoMemberProfileEntity memberProfile,
      FarmLandholdingEntity landholding,
      String plotName,
      BigDecimal areaAcres,
      BigDecimal latitude,
      BigDecimal longitude,
      String soilType,
      FarmRecordStatus status,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.memberProfile = memberProfile;
    this.landholding = landholding;
    this.plotName = plotName;
    this.areaAcres = areaAcres;
    this.latitude = latitude;
    this.longitude = longitude;
    this.soilType = soilType;
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

  public FarmLandholdingEntity getLandholding() {
    return landholding;
  }

  public String getPlotName() {
    return plotName;
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

  public String getSoilType() {
    return soilType;
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
      FarmLandholdingEntity landholding,
      String plotName,
      BigDecimal areaAcres,
      BigDecimal latitude,
      BigDecimal longitude,
      String soilType,
      FarmRecordStatus status,
      Instant now
  ) {
    this.landholding = landholding;
    this.plotName = plotName;
    this.areaAcres = areaAcres;
    this.latitude = latitude;
    this.longitude = longitude;
    this.soilType = soilType;
    this.status = status;
    this.updatedAt = now;
  }

  public void updateStatus(FarmRecordStatus status, Instant now) {
    this.status = status;
    this.updatedAt = now;
  }
}
