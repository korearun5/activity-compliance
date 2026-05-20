package com.activityplatform.backend.carbon.api;

import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CarbonFarmPlotRequest(
    @Size(max = 160)
    String farmName,
    @Size(max = 120)
    String surveyNumber,
    @NotNull(message = "Area is required.")
    @DecimalMin(value = "0.0001", message = "Area must be greater than zero.")
    @Digits(integer = 8, fraction = 4)
    BigDecimal areaAcres,
    @NotNull(message = "Latitude is required.")
    @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90.")
    @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90.")
    @Digits(integer = 3, fraction = 7)
    BigDecimal latitude,
    @NotNull(message = "Longitude is required.")
    @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180.")
    @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180.")
    @Digits(integer = 3, fraction = 7)
    BigDecimal longitude,
    @Size(max = 120)
    String irrigationSource,
    @Size(max = 160)
    String primaryCrop,
    @Size(max = 80)
    String tillageStatus,
    @Size(max = 255)
    String variety,
    @Size(max = 255)
    String rootstock,
    LocalDate plantingDate,
    @Size(max = 100)
    String blockCode,
    @Size(max = 50)
    String spacing,
    Integer rowCount,
    CarbonRecordStatus status
) {
}
