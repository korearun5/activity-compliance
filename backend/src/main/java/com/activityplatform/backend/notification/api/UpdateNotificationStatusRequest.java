package com.activityplatform.backend.notification.api;

import com.activityplatform.backend.notification.domain.NotificationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationStatusRequest(
    @NotNull NotificationStatus status
) {
}
