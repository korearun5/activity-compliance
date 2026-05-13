package com.activityplatform.backend.fpo.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CropHistoryRequest(
    @NotNull
    UUID cropId,
    UUID seasonId,
    @NotNull
    @Min(1900)
    @Max(2200)
    Integer cropYear,
    @DecimalMin(value = "0.0001", message = "Crop area must be greater than zero.")
    @Digits(integer = 8, fraction = 4)
    BigDecimal areaAcres,
    @DecimalMin(value = "0.0000", message = "Yield quantity cannot be negative.")
    @Digits(integer = 10, fraction = 4)
    BigDecimal yieldQuantity,
    @Size(max = 40)
    String yieldUnit,
    @Size(max = 2000)
    String notes
) {
}
