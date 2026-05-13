package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CropSeasonRequest(
    @NotBlank
    @Size(max = 80)
    String code,
    @NotBlank
    @Size(max = 160)
    String name,
    @Min(1)
    @Max(12)
    Integer startMonth,
    @Min(1)
    @Max(12)
    Integer endMonth,
    @NotNull
    @Min(1900)
    @Max(2200)
    Integer seasonYear,
    FarmRecordStatus status
) {
}
