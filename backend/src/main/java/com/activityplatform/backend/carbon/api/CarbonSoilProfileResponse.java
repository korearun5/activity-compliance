package com.activityplatform.backend.carbon.api;

import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import com.activityplatform.backend.carbon.domain.CarbonSoilProfileEntity;
import com.activityplatform.backend.carbon.domain.CarbonVerificationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CarbonSoilProfileResponse(
    UUID id,
    UUID tenantId,
    UUID carbonProfileId,
    String profileName,
    String profileMobileNumber,
    UUID carbonFarmPlotId,
    String farmName,
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
    CarbonVerificationStatus verificationStatus,
    String verificationNotes,
    Instant verifiedAt,
    UUID verifiedByUserId,
    CarbonRecordStatus status,
    Instant createdAt,
    Instant updatedAt
) {
  public static CarbonSoilProfileResponse from(CarbonSoilProfileEntity profile) {
    return new CarbonSoilProfileResponse(
        profile.getId(),
        profile.getTenant().getId(),
        profile.getCarbonProfile().getId(),
        profile.getCarbonProfile().getDisplayName(),
        profile.getCarbonProfile().getMobileNumber(),
        profile.getCarbonFarmPlot() == null ? null : profile.getCarbonFarmPlot().getId(),
        profile.getCarbonFarmPlot() == null ? null : profile.getCarbonFarmPlot().getFarmName(),
        profile.getTestDate(),
        profile.getLabName(),
        profile.getSoilOrganicCarbonPercent(),
        profile.getPh(),
        profile.getElectricalConductivity(),
        profile.getNitrogenKgHa(),
        profile.getPhosphorusKgHa(),
        profile.getPotassiumKgHa(),
        profile.getBulkDensityGmCm3(),
        profile.getTexture(),
        profile.getReportFileName(),
        profile.getReportContentType(),
        profile.getReportStorageKey(),
        profile.getReportUrl(),
        profile.getVerificationStatus(),
        profile.getVerificationNotes(),
        profile.getVerifiedAt(),
        profile.getVerifiedByUser() == null ? null : profile.getVerifiedByUser().getId(),
        profile.getStatus(),
        profile.getCreatedAt(),
        profile.getUpdatedAt()
    );
  }
}
