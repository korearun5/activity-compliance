package com.activityplatform.backend.carbon.api;

import com.activityplatform.backend.carbon.domain.CarbonActivityRecordEntity;
import com.activityplatform.backend.carbon.domain.CarbonActivityVerificationStatus;
import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CarbonActivityRecordResponse(
    UUID id,
    UUID tenantId,
    UUID carbonProfileId,
    UUID carbonFarmPlotId,
    String farmName,
    UUID categoryId,
    String categoryCode,
    String categoryName,
    boolean evidenceRequired,
    LocalDate activityDate,
    String cropName,
    String inputUsed,
    BigDecimal quantityValue,
    String quantityUnit,
    String remarks,
    int evidenceCount,
    CarbonActivityVerificationStatus verificationStatus,
    CarbonRecordStatus status,
    Instant createdAt,
    Instant updatedAt
) {
  public static CarbonActivityRecordResponse from(CarbonActivityRecordEntity record) {
    return new CarbonActivityRecordResponse(
        record.getId(),
        record.getTenant().getId(),
        record.getCarbonProfile().getId(),
        record.getCarbonFarmPlot() == null ? null : record.getCarbonFarmPlot().getId(),
        record.getCarbonFarmPlot() == null ? null : record.getCarbonFarmPlot().getFarmName(),
        record.getCategory().getId(),
        record.getCategory().getCode(),
        record.getCategory().getName(),
        record.getCategory().isEvidenceRequired(),
        record.getActivityDate(),
        record.getCropName(),
        record.getInputUsed(),
        record.getQuantityValue(),
        record.getQuantityUnit(),
        record.getRemarks(),
        record.getEvidenceCount(),
        record.getVerificationStatus(),
        record.getStatus(),
        record.getCreatedAt(),
        record.getUpdatedAt()
    );
  }
}
