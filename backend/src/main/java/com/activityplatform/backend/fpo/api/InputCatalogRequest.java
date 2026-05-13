package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InputCatalogRequest(
    @NotBlank
    @Size(max = 80)
    String code,
    @NotBlank
    @Size(max = 160)
    String name,
    @Size(max = 80)
    String category,
    @NotBlank
    @Size(max = 40)
    String unit,
    FarmRecordStatus status
) {
}
