package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateFarmPlotRequest(
    UUID landholdingId,
    @NotBlank
    @Size(max = 160)
    String plotName,
    @NotNull
    @DecimalMin(value = "0.0001", message = "Plot area must be greater than zero.")
    @Digits(integer = 8, fraction = 4)
    BigDecimal areaAcres,
    @NotNull
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90.")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90.")
    @Digits(integer = 3, fraction = 7)
    BigDecimal latitude,
    @NotNull
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180.")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180.")
    @Digits(integer = 4, fraction = 7)
    BigDecimal longitude,
    @Size(max = 120)
    String soilType,
    FarmRecordStatus status
) {
}
