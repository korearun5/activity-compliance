package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.FarmPlotEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FarmPlotResponse(
    UUID id,
    UUID tenantId,
    UUID memberId,
    String memberNumber,
    UUID landholdingId,
    String plotName,
    BigDecimal areaAcres,
    BigDecimal latitude,
    BigDecimal longitude,
    String soilType,
    FarmRecordStatus status,
    Instant createdAt,
    Instant updatedAt
) {
  public static FarmPlotResponse from(FarmPlotEntity plot) {
    return new FarmPlotResponse(
        plot.getId(),
        plot.getTenant().getId(),
        plot.getMemberProfile().getId(),
        plot.getMemberProfile().getMemberNumber(),
        plot.getLandholding() == null ? null : plot.getLandholding().getId(),
        plot.getPlotName(),
        plot.getAreaAcres(),
        plot.getLatitude(),
        plot.getLongitude(),
        plot.getSoilType(),
        plot.getStatus(),
        plot.getCreatedAt(),
        plot.getUpdatedAt()
    );
  }
}
