package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CropInputRuleRequest(
    @NotNull
    UUID cropId,
    @NotNull
    UUID inputId,
    @NotNull
    @DecimalMin(value = "0.0001", message = "Quantity per acre must be greater than zero.")
    @Digits(integer = 10, fraction = 4)
    BigDecimal quantityPerAcre,
    @Size(max = 120)
    String applicationStage,
    @Size(max = 2000)
    String notes,
    FarmRecordStatus status
) {
}
