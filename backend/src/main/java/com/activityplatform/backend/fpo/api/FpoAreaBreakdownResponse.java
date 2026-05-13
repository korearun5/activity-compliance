package com.activityplatform.backend.fpo.api;

import java.math.BigDecimal;
import java.util.UUID;

public record FpoAreaBreakdownResponse(
    UUID id,
    String label,
    BigDecimal areaAcres,
    long planCount,
    long memberCount
) {
}
