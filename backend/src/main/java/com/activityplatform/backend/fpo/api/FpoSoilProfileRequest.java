package com.activityplatform.backend.fpo.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FpoSoilProfileRequest(
    @DecimalMin(value = "0.0", message = "SOC cannot be negative.")
    @Digits(integer = 6, fraction = 4)
    BigDecimal soilOrganicCarbon,
    @DecimalMin(value = "0.0", message = "pH cannot be negative.")
    @DecimalMax(value = "14.0", message = "pH cannot be greater than 14.")
    @Digits(integer = 2, fraction = 2)
    BigDecimal ph,
    @DecimalMin(value = "0.0", message = "Nitrogen cannot be negative.")
    @Digits(integer = 8, fraction = 4)
    BigDecimal nitrogen,
    @DecimalMin(value = "0.0", message = "Phosphorus cannot be negative.")
    @Digits(integer = 8, fraction = 4)
    BigDecimal phosphorus,
    @DecimalMin(value = "0.0", message = "Potassium cannot be negative.")
    @Digits(integer = 8, fraction = 4)
    BigDecimal potassium,
    @Size(max = 240)
    String reportFileName,
    @Size(max = 120)
    String reportContentType,
    @Size(max = 1000)
    String reportUrl,
    @Size(max = 1000)
    String notes
) {
}
