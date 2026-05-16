package com.activityplatform.backend.carbon.api;

import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CarbonActivityRecordRequest(
    UUID carbonFarmPlotId,
    @NotNull(message = "Activity category is required.")
    UUID categoryId,
    @NotNull(message = "Activity date is required.")
    LocalDate activityDate,
    @NotBlank(message = "Crop name is required.")
    @Size(max = 160)
    String cropName,
    @Size(max = 180)
    String inputUsed,
    @DecimalMin(value = "0.0", message = "Quantity cannot be negative.")
    @Digits(integer = 8, fraction = 4)
    BigDecimal quantityValue,
    @Size(max = 40)
    String quantityUnit,
    @Size(max = 2000)
    String remarks,
    CarbonRecordStatus status
) {
}
