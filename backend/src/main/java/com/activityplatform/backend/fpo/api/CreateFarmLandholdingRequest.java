package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateFarmLandholdingRequest(
    @Size(max = 120)
    String surveyNumber,
    @NotNull
    @DecimalMin(value = "0.0001", message = "Total area must be greater than zero.")
    @Digits(integer = 8, fraction = 4)
    BigDecimal totalAreaAcres,
    @DecimalMin(value = "0.0", message = "Cultivable area cannot be negative.")
    @Digits(integer = 8, fraction = 4)
    BigDecimal cultivableAreaAcres,
    @Size(max = 80)
    String ownershipType,
    @Size(max = 120)
    String irrigationSource,
    FarmRecordStatus status
) {
}
