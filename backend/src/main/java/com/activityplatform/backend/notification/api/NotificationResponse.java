package com.activityplatform.backend.notification.api;

import com.activityplatform.backend.notification.domain.NotificationChannel;
import com.activityplatform.backend.notification.domain.NotificationEventEntity;
import com.activityplatform.backend.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    UUID tenantId,
    UUID recipientUserId,
    String recipientUsername,
    NotificationChannel channel,
    String templateCode,
    Map<String, Object> payload,
    NotificationStatus status,
    Instant queuedAt,
    Instant sentAt
) {
  public static NotificationResponse from(NotificationEventEntity notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getTenant().getId(),
        notification.getRecipient() == null ? null : notification.getRecipient().getId(),
        notification.getRecipient() == null ? null : notification.getRecipient().getUsername(),
        notification.getChannel(),
        notification.getTemplateCode(),
        notification.getPayload(),
        notification.getStatus(),
        notification.getQueuedAt(),
        notification.getSentAt()
    );
  }
}
