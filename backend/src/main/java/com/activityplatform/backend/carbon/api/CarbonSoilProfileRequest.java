package com.activityplatform.backend.carbon.api;

import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CarbonSoilProfileRequest(
    UUID carbonFarmPlotId,
    LocalDate testDate,
    @Size(max = 180)
    String labName,
    @DecimalMin(value = "0.0", message = "SOC cannot be negative.")
    @Digits(integer = 6, fraction = 4)
    BigDecimal soilOrganicCarbonPercent,
    @DecimalMin(value = "0.0", message = "pH cannot be negative.")
    @DecimalMax(value = "14.0", message = "pH cannot be greater than 14.")
    @Digits(integer = 2, fraction = 2)
    BigDecimal ph,
    @DecimalMin(value = "0.0", message = "EC cannot be negative.")
    @Digits(integer = 8, fraction = 4)
    BigDecimal electricalConductivity,
    @DecimalMin(value = "0.0", message = "Nitrogen cannot be negative.")
    @Digits(integer = 8, fraction = 4)
    BigDecimal nitrogenKgHa,
    @DecimalMin(value = "0.0", message = "Phosphorus cannot be negative.")
    @Digits(integer = 8, fraction = 4)
    BigDecimal phosphorusKgHa,
    @DecimalMin(value = "0.0", message = "Potassium cannot be negative.")
    @Digits(integer = 8, fraction = 4)
    BigDecimal potassiumKgHa,
    @DecimalMin(value = "0.0", message = "Bulk density cannot be negative.")
    @Digits(integer = 6, fraction = 4)
    BigDecimal bulkDensityGmCm3,
    @Size(max = 120)
    String texture,
    @Size(max = 240)
    String reportFileName,
    @Size(max = 120)
    String reportContentType,
    @Size(max = 1000)
    String reportStorageKey,
    @Size(max = 1000)
    String reportUrl,
    CarbonRecordStatus status
) {
}
