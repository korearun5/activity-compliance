import { apiClient, PaginationParams } from "../core/api/client";
import { PageResponse } from "../core/api/contracts";
import { endpoints } from "../core/api/endpoints";

export type NotificationChannel = "EMAIL" | "IN_APP" | "PUSH" | "SMS";

export type NotificationStatus = "FAILED" | "QUEUED" | "SENT" | "SKIPPED";

export type NotificationRecord = {
  channel: NotificationChannel;
  id: string;
  payload: Record<string, unknown>;
  queuedAt: string;
  recipientUserId: string | null;
  recipientUsername: string | null;
  sentAt: string | null;
  status: NotificationStatus;
  templateCode: string;
  tenantId: string;
};

export type QueueNotificationInput = {
  channel: NotificationChannel;
  payload?: Record<string, unknown>;
  recipientUserId?: string;
  templateCode: string;
};

export async function getBackendNotifications(
  status?: NotificationStatus,
  pagination: PaginationParams = {}
) {
  const path = status
    ? `${endpoints.notifications.list}?status=${encodeURIComponent(status)}`
    : endpoints.notifications.list;

  return apiClient.getPaginated<PageResponse<NotificationRecord>>(path, pagination);
}

export async function queueBackendNotification(input: QueueNotificationInput) {
  return apiClient.post<QueueNotificationInput, NotificationRecord>(
    endpoints.notifications.queue,
    input
  );
}

export async function updateBackendNotificationStatus(
  notificationId: string,
  status: NotificationStatus
) {
  return apiClient.patch<{ status: NotificationStatus }, NotificationRecord>(
    endpoints.notifications.status(notificationId),
    { status }
  );
}
