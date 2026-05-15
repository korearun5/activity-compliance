package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.AdvisoryStatus;
import com.activityplatform.backend.fpo.domain.AdvisoryTargetType;
import com.activityplatform.backend.fpo.domain.AdvisoryCategory;
import com.activityplatform.backend.notification.domain.NotificationChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record FpoAdvisoryRequest(
    UUID cropId,
    UUID seasonId,
    @NotNull
    AdvisoryCategory category,
    AdvisoryTargetType targetType,
    @NotBlank
    @Size(max = 180)
    String title,
    @NotBlank
    String message,
    NotificationChannel channel,
    AdvisoryStatus status,
    @Size(max = 10)
    List<@Valid FpoAdvisoryImageRequest> images
) {
}
