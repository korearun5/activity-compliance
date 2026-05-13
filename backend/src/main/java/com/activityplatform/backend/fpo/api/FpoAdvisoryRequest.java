package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.AdvisoryStatus;
import com.activityplatform.backend.fpo.domain.AdvisoryTargetType;
import com.activityplatform.backend.notification.domain.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record FpoAdvisoryRequest(
    UUID cropId,
    UUID seasonId,
    AdvisoryTargetType targetType,
    @Size(max = 160)
    String targetVillage,
    UUID targetMemberId,
    @NotBlank
    @Size(max = 180)
    String title,
    @NotBlank
    String message,
    NotificationChannel channel,
    AdvisoryStatus status
) {
}
