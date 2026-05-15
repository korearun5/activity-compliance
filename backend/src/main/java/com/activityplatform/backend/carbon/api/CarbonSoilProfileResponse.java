package com.activityplatform.backend.carbon.api;

import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import com.activityplatform.backend.carbon.domain.CarbonSoilProfileEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CarbonSoilProfileResponse(
    UUID id,
    UUID tenantId,
    UUID carbonProfileId,
    UUID carbonFarmPlotId,
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
    Instant createdAt,
    Instant updatedAt
) {
  public static CarbonSoilProfileResponse from(CarbonSoilProfileEntity profile) {
    return new CarbonSoilProfileResponse(
        profile.getId(),
        profile.getTenant().getId(),
        profile.getCarbonProfile().getId(),
        profile.getCarbonFarmPlot() == null ? null : profile.getCarbonFarmPlot().getId(),
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
        profile.getStatus(),
        profile.getCreatedAt(),
        profile.getUpdatedAt()
    );
  }
}
