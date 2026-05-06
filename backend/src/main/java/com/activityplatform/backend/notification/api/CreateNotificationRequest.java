package com.activityplatform.backend.notification.api;

import com.activityplatform.backend.notification.domain.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

public record CreateNotificationRequest(
    UUID recipientUserId,
    @NotNull NotificationChannel channel,
    @NotBlank
    @Size(max = 80)
    @Pattern(regexp = "^[A-Z0-9][A-Z0-9_\\-]*$")
    String templateCode,
    Map<String, Object> payload
) {
}
