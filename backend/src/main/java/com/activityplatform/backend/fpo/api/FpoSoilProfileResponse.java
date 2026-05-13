package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.FpoSoilProfileEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FpoSoilProfileResponse(
    UUID id,
    UUID tenantId,
    UUID memberId,
    String memberNumber,
    BigDecimal soilOrganicCarbon,
    BigDecimal ph,
    BigDecimal nitrogen,
    BigDecimal phosphorus,
    BigDecimal potassium,
    String reportFileName,
    String reportContentType,
    String reportUrl,
    String notes,
    Instant createdAt,
    Instant updatedAt
) {
  public static FpoSoilProfileResponse from(FpoSoilProfileEntity profile) {
    return new FpoSoilProfileResponse(
        profile.getId(),
        profile.getTenant().getId(),
        profile.getMemberProfile().getId(),
        profile.getMemberProfile().getMemberNumber(),
        profile.getSoilOrganicCarbon(),
        profile.getPh(),
        profile.getNitrogen(),
        profile.getPhosphorus(),
        profile.getPotassium(),
        profile.getReportFileName(),
        profile.getReportContentType(),
        profile.getReportUrl(),
        profile.getNotes(),
        profile.getCreatedAt(),
        profile.getUpdatedAt()
    );
  }
}
