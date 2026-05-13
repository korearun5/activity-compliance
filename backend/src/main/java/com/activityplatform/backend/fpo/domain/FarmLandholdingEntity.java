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
@Table(name = "farm_landholdings")
public class FarmLandholdingEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_profile_id", nullable = false)
  private FpoMemberProfileEntity memberProfile;

  @Column(name = "survey_number")
  private String surveyNumber;

  @Column(name = "total_area_acres", nullable = false)
  private BigDecimal totalAreaAcres;

  @Column(name = "cultivable_area_acres")
  private BigDecimal cultivableAreaAcres;

  @Column(name = "ownership_type")
  private String ownershipType;

  @Column(name = "irrigation_source")
  private String irrigationSource;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FarmRecordStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected FarmLandholdingEntity() {
  }

  public FarmLandholdingEntity(
      UUID id,
      TenantEntity tenant,
      FpoMemberProfileEntity memberProfile,
      String surveyNumber,
      BigDecimal totalAreaAcres,
      BigDecimal cultivableAreaAcres,
      String ownershipType,
      String irrigationSource,
      FarmRecordStatus status,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.memberProfile = memberProfile;
    this.surveyNumber = surveyNumber;
    this.totalAreaAcres = totalAreaAcres;
    this.cultivableAreaAcres = cultivableAreaAcres;
    this.ownershipType = ownershipType;
    this.irrigationSource = irrigationSource;
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

  public String getSurveyNumber() {
    return surveyNumber;
  }

  public BigDecimal getTotalAreaAcres() {
    return totalAreaAcres;
  }

  public BigDecimal getCultivableAreaAcres() {
    return cultivableAreaAcres;
  }

  public String getOwnershipType() {
    return ownershipType;
  }

  public String getIrrigationSource() {
    return irrigationSource;
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
      String surveyNumber,
      BigDecimal totalAreaAcres,
      BigDecimal cultivableAreaAcres,
      String ownershipType,
      String irrigationSource,
      FarmRecordStatus status,
      Instant now
  ) {
    this.surveyNumber = surveyNumber;
    this.totalAreaAcres = totalAreaAcres;
    this.cultivableAreaAcres = cultivableAreaAcres;
    this.ownershipType = ownershipType;
    this.irrigationSource = irrigationSource;
    this.status = status;
    this.updatedAt = now;
  }

  public void updateStatus(FarmRecordStatus status, Instant now) {
    this.status = status;
    this.updatedAt = now;
  }
}
