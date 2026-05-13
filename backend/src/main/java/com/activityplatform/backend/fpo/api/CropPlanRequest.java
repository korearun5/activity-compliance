package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CropPlanRequest(
    @NotNull
    UUID memberId,
    UUID plotId,
    @NotNull
    UUID cropId,
    @NotNull
    UUID seasonId,
    @NotNull
    @DecimalMin(value = "0.0001", message = "Planned area must be greater than zero.")
    @Digits(integer = 8, fraction = 4)
    BigDecimal plannedAreaAcres,
    LocalDate plannedSowingDate,
    LocalDate expectedHarvestDate,
    CropPlanStatus status
) {
}
