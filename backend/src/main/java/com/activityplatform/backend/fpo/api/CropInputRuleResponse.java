package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.CropInputRuleEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CropInputRuleResponse(
    UUID id,
    UUID tenantId,
    UUID cropId,
    String cropCode,
    String cropName,
    UUID inputId,
    String inputCode,
    String inputName,
    String inputUnit,
    BigDecimal quantityPerAcre,
    String applicationStage,
    String notes,
    FarmRecordStatus status,
    Instant createdAt,
    Instant updatedAt
) {
  public static CropInputRuleResponse from(CropInputRuleEntity rule) {
    return new CropInputRuleResponse(
        rule.getId(),
        rule.getTenant().getId(),
        rule.getCrop().getId(),
        rule.getCrop().getCode(),
        rule.getCrop().getName(),
        rule.getInput().getId(),
        rule.getInput().getCode(),
        rule.getInput().getName(),
        rule.getInput().getUnit(),
        rule.getQuantityPerAcre(),
        rule.getApplicationStage(),
        rule.getNotes(),
        rule.getStatus(),
        rule.getCreatedAt(),
        rule.getUpdatedAt()
    );
  }
}
