package com.activityplatform.backend.fpo.api;

import java.math.BigDecimal;
import java.util.UUID;

public record FpoInputDemandBreakdownResponse(
    UUID inputId,
    String inputCode,
    String inputName,
    String unit,
    BigDecimal estimatedQuantity,
    long planCount,
    long memberCount
) {
}
