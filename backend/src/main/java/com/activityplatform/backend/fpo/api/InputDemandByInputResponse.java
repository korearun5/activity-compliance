package com.activityplatform.backend.fpo.api;

import java.math.BigDecimal;
import java.util.UUID;

public record InputDemandByInputResponse(
    UUID inputId,
    String inputCode,
    String inputName,
    String unit,
    BigDecimal estimatedQuantity,
    BigDecimal totalDemandQuantity,
    BigDecimal bufferQuantity,
    BigDecimal finalDemandQuantity,
    int planCount
) {
}
