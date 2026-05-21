package com.activityplatform.backend.carbon.domain;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
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
@Table(name = "carbon_soil_profiles")
public class CarbonSoilProfileEntity {
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

  @Column(name = "test_date")
  private LocalDate testDate;

  @Column(name = "lab_name")
  private String labName;

  @Column(name = "soil_organic_carbon_percent")
  private BigDecimal soilOrganicCarbonPercent;

  private BigDecimal ph;

  @Column(name = "ec")
  private BigDecimal electricalConductivity;

  @Column(name = "nitrogen_kg_ha")
  private BigDecimal nitrogenKgHa;

  @Column(name = "phosphorus_kg_ha")
  private BigDecimal phosphorusKgHa;

  @Column(name = "potassium_kg_ha")
  private BigDecimal potassiumKgHa;

  @Column(name = "bulk_density_g_cm3")
  private BigDecimal bulkDensityGmCm3;

  private String texture;

  @Column(name = "report_file_name")
  private String reportFileName;

  @Column(name = "report_content_type")
  private String reportContentType;

  @Column(name = "report_storage_key")
  private String reportStorageKey;

  @Column(name = "report_url")
  private String reportUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status", nullable = false)
  private CarbonVerificationStatus verificationStatus;

  @Column(name = "verification_notes")
  private String verificationNotes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "verified_by_user_id")
  private UserEntity verifiedByUser;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CarbonRecordStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected CarbonSoilProfileEntity() {
  }

  public CarbonSoilProfileEntity(
      UUID id,
      TenantEntity tenant,
      CarbonProfileEntity carbonProfile,
      CarbonFarmPlotEntity carbonFarmPlot,
      LocalDate testDate,
      String labName,
      BigDecimal soilOrganicCarbonPercent,
      BigDecimal ph,
      BigDecimal electricalConductivity,
      BigDecimal nitrogenKgHa,
      BigDecimal phosphorusKgHa,
      BigDecimal potassiumKgHa,
      BigDecimal bulkDensityGmCm3,
      String texture,
      String reportFileName,
      String reportContentType,
      String reportStorageKey,
      String reportUrl,
      CarbonRecordStatus status,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.carbonProfile = carbonProfile;
    this.carbonFarmPlot = carbonFarmPlot;
    this.testDate = testDate;
    this.labName = labName;
    this.soilOrganicCarbonPercent = soilOrganicCarbonPercent;
    this.ph = ph;
    this.electricalConductivity = electricalConductivity;
    this.nitrogenKgHa = nitrogenKgHa;
    this.phosphorusKgHa = phosphorusKgHa;
    this.potassiumKgHa = potassiumKgHa;
    this.bulkDensityGmCm3 = bulkDensityGmCm3;
    this.texture = texture;
    this.reportFileName = reportFileName;
    this.reportContentType = reportContentType;
    this.reportStorageKey = reportStorageKey;
    this.reportUrl = reportUrl;
    this.verificationStatus = CarbonVerificationStatus.PENDING_VERIFICATION;
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

  public LocalDate getTestDate() {
    return testDate;
  }

  public String getLabName() {
    return labName;
  }

  public BigDecimal getSoilOrganicCarbonPercent() {
    return soilOrganicCarbonPercent;
  }

  public BigDecimal getPh() {
    return ph;
  }

  public BigDecimal getElectricalConductivity() {
    return electricalConductivity;
  }

  public BigDecimal getNitrogenKgHa() {
    return nitrogenKgHa;
  }

  public BigDecimal getPhosphorusKgHa() {
    return phosphorusKgHa;
  }

  public BigDecimal getPotassiumKgHa() {
    return potassiumKgHa;
  }

  public BigDecimal getBulkDensityGmCm3() {
    return bulkDensityGmCm3;
  }

  public String getTexture() {
    return texture;
  }

  public String getReportFileName() {
    return reportFileName;
  }

  public String getReportContentType() {
    return reportContentType;
  }

  public String getReportStorageKey() {
    return reportStorageKey;
  }

  public String getReportUrl() {
    return reportUrl;
  }

  public CarbonVerificationStatus getVerificationStatus() {
    return verificationStatus;
  }

  public String getVerificationNotes() {
    return verificationNotes;
  }

  public UserEntity getVerifiedByUser() {
    return verifiedByUser;
  }

  public Instant getVerifiedAt() {
    return verifiedAt;
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
      LocalDate testDate,
      String labName,
      BigDecimal soilOrganicCarbonPercent,
      BigDecimal ph,
      BigDecimal electricalConductivity,
      BigDecimal nitrogenKgHa,
      BigDecimal phosphorusKgHa,
      BigDecimal potassiumKgHa,
      BigDecimal bulkDensityGmCm3,
      String texture,
      String reportFileName,
      String reportContentType,
      String reportStorageKey,
      String reportUrl,
      CarbonRecordStatus status,
      Instant now
  ) {
    this.carbonFarmPlot = carbonFarmPlot;
    this.testDate = testDate;
    this.labName = labName;
    this.soilOrganicCarbonPercent = soilOrganicCarbonPercent;
    this.ph = ph;
    this.electricalConductivity = electricalConductivity;
    this.nitrogenKgHa = nitrogenKgHa;
    this.phosphorusKgHa = phosphorusKgHa;
    this.potassiumKgHa = potassiumKgHa;
    this.bulkDensityGmCm3 = bulkDensityGmCm3;
    this.texture = texture;
    this.reportFileName = reportFileName;
    this.reportContentType = reportContentType;
    this.reportStorageKey = reportStorageKey;
    this.reportUrl = reportUrl;
    resetVerification();
    this.status = status;
    this.updatedAt = now;
  }

  public void attachReport(
      String reportFileName,
      String reportContentType,
      String reportStorageKey,
      Instant now
  ) {
    this.reportFileName = reportFileName;
    this.reportContentType = reportContentType;
    this.reportStorageKey = reportStorageKey;
    this.reportUrl = null;
    resetVerification();
    this.updatedAt = now;
  }

  public void verify(
      CarbonVerificationStatus verificationStatus,
      String verificationNotes,
      UserEntity verifiedByUser,
      Instant now
  ) {
    this.verificationStatus = verificationStatus;
    this.verificationNotes = verificationNotes;
    this.verifiedByUser = verifiedByUser;
    this.verifiedAt = now;
    this.updatedAt = now;
  }

  private void resetVerification() {
    this.verificationStatus = CarbonVerificationStatus.PENDING_VERIFICATION;
    this.verificationNotes = null;
    this.verifiedByUser = null;
    this.verifiedAt = null;
  }
}
