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
@Table(name = "fpo_soil_profiles")
public class FpoSoilProfileEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_profile_id", nullable = false)
  private FpoMemberProfileEntity memberProfile;

  @Column(name = "soil_organic_carbon")
  private BigDecimal soilOrganicCarbon;

  private BigDecimal ph;

  @Column(name = "nitrogen")
  private BigDecimal nitrogen;

  @Column(name = "phosphorus")
  private BigDecimal phosphorus;

  @Column(name = "potassium")
  private BigDecimal potassium;

  @Column(name = "report_file_name")
  private String reportFileName;

  @Column(name = "report_content_type")
  private String reportContentType;

  @Column(name = "report_url")
  private String reportUrl;

  private String notes;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected FpoSoilProfileEntity() {
  }

  public FpoSoilProfileEntity(
      UUID id,
      TenantEntity tenant,
      FpoMemberProfileEntity memberProfile,
      BigDecimal soilOrganicCarbon,
      BigDecimal ph,
      BigDecimal nitrogen,
      BigDecimal phosphorus,
      BigDecimal potassium,
      String reportFileName,
      String reportContentType,
      String reportUrl,
      String notes,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.memberProfile = memberProfile;
    this.soilOrganicCarbon = soilOrganicCarbon;
    this.ph = ph;
    this.nitrogen = nitrogen;
    this.phosphorus = phosphorus;
    this.potassium = potassium;
    this.reportFileName = reportFileName;
    this.reportContentType = reportContentType;
    this.reportUrl = reportUrl;
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

  public BigDecimal getSoilOrganicCarbon() {
    return soilOrganicCarbon;
  }

  public BigDecimal getPh() {
    return ph;
  }

  public BigDecimal getNitrogen() {
    return nitrogen;
  }

  public BigDecimal getPhosphorus() {
    return phosphorus;
  }

  public BigDecimal getPotassium() {
    return potassium;
  }

  public String getReportFileName() {
    return reportFileName;
  }

  public String getReportContentType() {
    return reportContentType;
  }

  public String getReportUrl() {
    return reportUrl;
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
      BigDecimal soilOrganicCarbon,
      BigDecimal ph,
      BigDecimal nitrogen,
      BigDecimal phosphorus,
      BigDecimal potassium,
      String reportFileName,
      String reportContentType,
      String reportUrl,
      String notes,
      Instant now
  ) {
    this.soilOrganicCarbon = soilOrganicCarbon;
    this.ph = ph;
    this.nitrogen = nitrogen;
    this.phosphorus = phosphorus;
    this.potassium = potassium;
    this.reportFileName = reportFileName;
    this.reportContentType = reportContentType;
    this.reportUrl = reportUrl;
    this.notes = notes;
    this.updatedAt = now;
  }
}
