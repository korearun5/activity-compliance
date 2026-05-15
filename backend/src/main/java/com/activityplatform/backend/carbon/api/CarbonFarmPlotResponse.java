package com.activityplatform.backend.carbon.api;

import com.activityplatform.backend.carbon.domain.CarbonFarmPlotEntity;
import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CarbonFarmPlotResponse(
    UUID id,
    UUID tenantId,
    UUID carbonProfileId,
    String farmName,
    String surveyNumber,
    BigDecimal areaAcres,
    BigDecimal latitude,
    BigDecimal longitude,
    String irrigationSource,
    String primaryCrop,
    String tillageStatus,
    CarbonRecordStatus status,
    Instant createdAt,
    Instant updatedAt
) {
  public static CarbonFarmPlotResponse from(CarbonFarmPlotEntity plot) {
    return new CarbonFarmPlotResponse(
        plot.getId(),
        plot.getTenant().getId(),
        plot.getCarbonProfile().getId(),
        plot.getFarmName(),
        plot.getSurveyNumber(),
        plot.getAreaAcres(),
        plot.getLatitude(),
        plot.getLongitude(),
        plot.getIrrigationSource(),
        plot.getPrimaryCrop(),
        plot.getTillageStatus(),
        plot.getStatus(),
        plot.getCreatedAt(),
        plot.getUpdatedAt()
    );
  }
}
