import { useEffect, useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { getErrorMessage } from "../core/errors/AppError";
import { RegisteredParticipant } from "../data/adminRegistryStore";
import {
  getBackendNotifications,
  NotificationChannel,
  NotificationRecord,
  NotificationStatus,
  queueBackendNotification,
  updateBackendNotificationStatus
} from "../data/notificationStore";
import { StatusBadge } from "../ui/StatusBadge";

type AdminNotificationsTabProps = {
  canUseBackend: boolean;
  participants: RegisteredParticipant[];
};

const channelOptions: NotificationChannel[] = ["IN_APP", "SMS", "EMAIL", "PUSH"];
const statusFilters: Array<NotificationStatus | "ALL"> = [
  "ALL",
  "QUEUED",
  "SENT",
  "FAILED",
  "SKIPPED"
];
const updateStatuses: NotificationStatus[] = ["SENT", "FAILED", "SKIPPED"];

export function AdminNotificationsTab({
  canUseBackend,
  participants
}: AdminNotificationsTabProps) {
  const [channel, setChannel] = useState<NotificationChannel>("IN_APP");
  const [error, setError] = useState("");
  const [filter, setFilter] = useState<NotificationStatus | "ALL">("ALL");
  const [isLoading, setIsLoading] = useState(false);
  const [isQueueing, setIsQueueing] = useState(false);
  const [message, setMessage] = useState("");
  const [notifications, setNotifications] = useState<NotificationRecord[]>([]);
  const [recipientUserId, setRecipientUserId] = useState("");
  const [templateCode, setTemplateCode] = useState("ACTIVITY_STATUS");
  const [updatingNotificationId, setUpdatingNotificationId] = useState<string | null>(
    null
  );

  useEffect(() => {
    if (!canUseBackend) {
      setNotifications([]);
      return;
    }

    loadNotifications(filter);
  }, [canUseBackend, filter]);

  async function loadNotifications(nextFilter = filter) {
    setIsLoading(true);
    setError("");

    try {
      const page = await getBackendNotifications(
        nextFilter === "ALL" ? undefined : nextFilter,
        { size: 50, sort: "queuedAt,desc" }
      );
      setNotifications(page.content);
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Unable to load notifications."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleQueueNotification() {
    const normalizedTemplate = templateCode.trim().toUpperCase();

    if (!/^[A-Z0-9][A-Z0-9_-]*$/.test(normalizedTemplate)) {
      setError("Template code must use uppercase letters, numbers, underscores, or hyphens.");
      return;
    }

    setIsQueueing(true);
    setError("");

    try {
      const notification = await queueBackendNotification({
        channel,
        payload: message.trim() ? { message: message.trim() } : {},
        recipientUserId: recipientUserId || undefined,
        templateCode: normalizedTemplate
      });
      setNotifications((currentNotifications) => [
        notification,
        ...currentNotifications.filter((item) => item.id !== notification.id)
      ]);
      setMessage("");
      setRecipientUserId("");
      setTemplateCode("ACTIVITY_STATUS");
    } catch (queueError) {
      setError(getErrorMessage(queueError, "Unable to queue notification."));
    } finally {
      setIsQueueing(false);
    }
  }

  async function handleUpdateStatus(
    notificationId: string,
    status: NotificationStatus
  ) {
    setUpdatingNotificationId(notificationId);
    setError("");

    try {
      const notification = await updateBackendNotificationStatus(
        notificationId,
        status
      );
      setNotifications((currentNotifications) =>
        currentNotifications.map((item) =>
          item.id === notification.id ? notification : item
        )
      );
    } catch (updateError) {
      setError(getErrorMessage(updateError, "Unable to update notification."));
    } finally {
      setUpdatingNotificationId(null);
    }
  }

  return (
    <View style={styles.section}>
      {!canUseBackend ? (
        <View style={styles.warningCard}>
          <Text style={styles.warningText}>
            Backend session is required for notification tracking.
          </Text>
        </View>
      ) : null}

      <View style={styles.managementCard}>
        <Text style={styles.cardTitle}>Queue notification</Text>
        <View style={styles.formGrid}>
          <NotificationField
            label="Template"
            value={templateCode}
            onChange={setTemplateCode}
          />
          <NotificationField label="Message" value={message} onChange={setMessage} />
        </View>

        <Text style={styles.formLabel}>Channel</Text>
        <View style={styles.choiceRow}>
          {channelOptions.map((option) => (
            <Pressable
              accessibilityRole="button"
              key={option}
              style={[
                styles.choiceButton,
                channel === option && styles.choiceButtonActive
              ]}
              onPress={() => setChannel(option)}
            >
              <Text
                style={[
                  styles.choiceButtonText,
                  channel === option && styles.choiceButtonTextActive
                ]}
              >
                {channelLabel(option)}
              </Text>
            </Pressable>
          ))}
        </View>

        <Text style={styles.formLabel}>Recipient</Text>
        <View style={styles.choiceRow}>
          <Pressable
            accessibilityRole="button"
            style={[
              styles.choiceButton,
              !recipientUserId && styles.choiceButtonActive
            ]}
            onPress={() => setRecipientUserId("")}
          >
            <Text
              style={[
                styles.choiceButtonText,
                !recipientUserId && styles.choiceButtonTextActive
              ]}
            >
              Tenant broadcast
            </Text>
          </Pressable>
          {participants
            .filter((participant) => participant.id)
            .map((participant) => (
              <Pressable
                accessibilityRole="button"
                key={participant.id}
                style={[
                  styles.choiceButton,
                  recipientUserId === participant.id && styles.choiceButtonActive
                ]}
                onPress={() => setRecipientUserId(participant.id ?? "")}
              >
                <Text
                  style={[
                    styles.choiceButtonText,
                    recipientUserId === participant.id &&
                      styles.choiceButtonTextActive
                  ]}
                >
                  {participant.name}
                </Text>
                <Text style={styles.choiceButtonMeta}>{participant.region}</Text>
              </Pressable>
            ))}
        </View>

        <View style={styles.formActions}>
          <Pressable
            accessibilityRole="button"
            disabled={!canUseBackend || isQueueing}
            style={[styles.primaryButton, isQueueing && styles.disabledButton]}
            onPress={handleQueueNotification}
          >
            <Text style={styles.primaryButtonText}>
              {isQueueing ? "Queueing..." : "Queue notification"}
            </Text>
          </Pressable>
        </View>
      </View>

      <View style={styles.headerRow}>
        <Text style={styles.sectionTitle}>Notification status</Text>
        <Pressable
          accessibilityRole="button"
          disabled={!canUseBackend || isLoading}
          style={[styles.secondaryButton, isLoading && styles.disabledButton]}
          onPress={() => loadNotifications()}
        >
          <Text style={styles.secondaryButtonText}>
            {isLoading ? "Refreshing..." : "Refresh"}
          </Text>
        </Pressable>
      </View>

      <View style={styles.filterRow}>
        {statusFilters.map((status) => (
          <Pressable
            accessibilityRole="button"
            key={status}
            style={[styles.filterButton, filter === status && styles.filterActive]}
            onPress={() => setFilter(status)}
          >
            <Text
              style={[
                styles.filterButtonText,
                filter === status && styles.filterButtonTextActive
              ]}
            >
              {statusLabel(status)}
            </Text>
          </Pressable>
        ))}
      </View>

      {error ? <Text style={styles.formError}>{error}</Text> : null}

      {notifications.length ? (
        notifications.map((notification) => (
          <View key={notification.id} style={styles.notificationCard}>
            <View style={styles.notificationText}>
              <Text style={styles.cardTitle}>{notification.templateCode}</Text>
              <Text style={styles.cardDescription}>
                {channelLabel(notification.channel)} -{" "}
                {notification.recipientUsername ?? "Tenant broadcast"}
              </Text>
              <Text style={styles.cardMeta}>
                Queued {formatDate(notification.queuedAt)}
                {notification.sentAt ? ` - Updated ${formatDate(notification.sentAt)}` : ""}
              </Text>
              {Object.keys(notification.payload ?? {}).length ? (
                <Text style={styles.cardDescription}>
                  {JSON.stringify(notification.payload)}
                </Text>
              ) : null}
            </View>
            <View style={styles.statusActions}>
              <StatusBadge
                label={statusLabel(notification.status)}
                tone={statusTone(notification.status)}
              />
              {notification.status === "QUEUED" ? (
                <View style={styles.actionRow}>
                  {updateStatuses.map((status) => (
                    <Pressable
                      accessibilityRole="button"
                      disabled={updatingNotificationId === notification.id}
                      key={status}
                      style={[
                        styles.statusButton,
                        updatingNotificationId === notification.id &&
                          styles.disabledButton
                      ]}
                      onPress={() => handleUpdateStatus(notification.id, status)}
                    >
                      <Text style={styles.statusButtonText}>{statusLabel(status)}</Text>
                    </Pressable>
                  ))}
                </View>
              ) : null}
            </View>
          </View>
        ))
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.cardDescription}>
            {isLoading ? "Loading notifications..." : "No notifications found."}
          </Text>
        </View>
      )}
    </View>
  );
}

function NotificationField({
  label,
  onChange,
  value
}: {
  label: string;
  onChange: (value: string) => void;
  value: string;
}) {
  return (
    <View style={styles.formField}>
      <Text style={styles.formLabel}>{label}</Text>
      <TextInput
        autoCapitalize={label === "Template" ? "characters" : "sentences"}
        autoCorrect={label !== "Template"}
        onChangeText={onChange}
        style={styles.formInput}
        value={value}
      />
    </View>
  );
}

function channelLabel(channel: NotificationChannel) {
  switch (channel) {
    case "IN_APP":
      return "In app";
    case "SMS":
      return "SMS";
    case "EMAIL":
      return "Email";
    default:
      return "Push";
  }
}

function statusLabel(status: NotificationStatus | "ALL") {
  switch (status) {
    case "ALL":
      return "All";
    case "QUEUED":
      return "Queued";
    case "SENT":
      return "Sent";
    case "FAILED":
      return "Failed";
    default:
      return "Skipped";
  }
}

function statusTone(status: NotificationStatus) {
  switch (status) {
    case "SENT":
      return "good";
    case "FAILED":
      return "danger";
    case "SKIPPED":
      return "neutral";
    default:
      return "warning";
  }
}

function formatDate(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString("en-IN", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    month: "short",
    year: "numeric"
  });
}

const styles = StyleSheet.create({
  section: {
    gap: 14
  },
  headerRow: {
    alignItems: "center",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  sectionTitle: {
    color: "#172126",
    fontSize: 18,
    fontWeight: "800"
  },
  managementCard: {
    backgroundColor: "#ffffff",
    borderColor: "#cfe0df",
    borderRadius: 8,
    borderWidth: 1,
    gap: 14,
    padding: 16
  },
  notificationCard: {
    alignItems: "flex-start",
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    padding: 16
  },
  notificationText: {
    flex: 1
  },
  cardTitle: {
    color: "#172126",
    fontSize: 17,
    fontWeight: "800",
    marginBottom: 5
  },
  cardDescription: {
    color: "#53666f",
    fontSize: 14,
    lineHeight: 20
  },
  cardMeta: {
    color: "#6d7f88",
    fontSize: 13,
    fontWeight: "700",
    marginTop: 7
  },
  formGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
  },
  formField: {
    flex: 1,
    gap: 7,
    minWidth: 210
  },
  formLabel: {
    color: "#24343b",
    fontSize: 13,
    fontWeight: "800"
  },
  formInput: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    color: "#172126",
    fontSize: 15,
    minHeight: 48,
    paddingHorizontal: 12
  },
  choiceRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  choiceButton: {
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    minWidth: 128,
    paddingHorizontal: 12,
    paddingVertical: 10
  },
  choiceButtonActive: {
    backgroundColor: "#e8f3f2",
    borderColor: "#1f6f73"
  },
  choiceButtonText: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800"
  },
  choiceButtonTextActive: {
    color: "#1f6f73"
  },
  choiceButtonMeta: {
    color: "#6d7f88",
    fontSize: 12,
    fontWeight: "700",
    marginTop: 4
  },
  formActions: {
    alignItems: "flex-start"
  },
  primaryButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 44,
    minWidth: 148,
    paddingHorizontal: 16
  },
  primaryButtonText: {
    color: "#ffffff",
    fontSize: 14,
    fontWeight: "800"
  },
  secondaryButton: {
    alignItems: "center",
    borderColor: "#1f6f73",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 40,
    minWidth: 108,
    paddingHorizontal: 12
  },
  secondaryButtonText: {
    color: "#1f6f73",
    fontSize: 13,
    fontWeight: "800"
  },
  disabledButton: {
    opacity: 0.6
  },
  filterRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8
  },
  filterButton: {
    alignItems: "center",
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 38,
    minWidth: 82,
    paddingHorizontal: 10
  },
  filterActive: {
    backgroundColor: "#e8f3f2",
    borderColor: "#1f6f73"
  },
  filterButtonText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  filterButtonTextActive: {
    color: "#1f6f73"
  },
  statusActions: {
    alignItems: "flex-end",
    gap: 10
  },
  actionRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    justifyContent: "flex-end",
    maxWidth: 270
  },
  statusButton: {
    alignItems: "center",
    borderColor: "#1f6f73",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 36,
    minWidth: 76,
    paddingHorizontal: 10
  },
  statusButtonText: {
    color: "#1f6f73",
    fontSize: 12,
    fontWeight: "800"
  },
  warningCard: {
    backgroundColor: "#fff8e8",
    borderColor: "#f0d38a",
    borderRadius: 8,
    borderWidth: 1,
    padding: 14
  },
  warningText: {
    color: "#8a5a00",
    fontSize: 13,
    fontWeight: "700"
  },
  formError: {
    color: "#b42318",
    fontSize: 13,
    fontWeight: "700"
  },
  emptyCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    padding: 16
  }
});
