import { StyleSheet, Text, View } from "react-native";

import { EvidenceStatus } from "../../core/model/types";
import { StatusBadge } from "../../ui/StatusBadge";
import { ApprovalActions } from "./ApprovalActions";

export type EvidenceReviewItem = {
  description?: string;
  id: string;
  note?: string;
  status: EvidenceStatus;
  submittedLabel: string;
  title: string;
};

type EvidenceReviewQueueProps<T extends EvidenceReviewItem> = {
  canReview: boolean;
  emptyMessage: string;
  error?: string;
  items: T[];
  module: "carbon" | "fpo";
  onReview: (item: T, status: "APPROVED" | "REJECTED") => void;
  reviewingItemId: string | null;
};

export function EvidenceReviewQueue<T extends EvidenceReviewItem>({
  canReview,
  emptyMessage,
  error,
  items,
  module,
  onReview,
  reviewingItemId
}: EvidenceReviewQueueProps<T>) {
  return (
    <View style={styles.queue} testID={`${module}-evidence-review-queue`}>
      {error ? <Text style={styles.errorText}>{error}</Text> : null}
      {items.length ? (
        items.map((item) => {
          const isSubmitting = reviewingItemId === item.id;
          const canReviewItem =
            canReview && (item.status === "pending" || item.status === "done");

          return (
            <View key={item.id} style={styles.reviewCard}>
              <View style={styles.reviewText}>
                <Text style={styles.title}>{item.title}</Text>
                {item.description ? (
                  <Text style={styles.description}>{item.description}</Text>
                ) : null}
                <Text style={styles.meta}>{item.submittedLabel}</Text>
                {item.note ? <Text style={styles.description}>{item.note}</Text> : null}
              </View>
              <View style={styles.reviewActions}>
                <StatusBadge
                  label={evidenceStatusLabel(item.status)}
                  tone={evidenceStatusTone(item.status)}
                />
                {canReviewItem ? (
                  <ApprovalActions
                    isSubmitting={isSubmitting}
                    onApprove={() => onReview(item, "APPROVED")}
                    onReject={() => onReview(item, "REJECTED")}
                  />
                ) : null}
              </View>
            </View>
          );
        })
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.description}>{emptyMessage}</Text>
        </View>
      )}
    </View>
  );
}

function evidenceStatusLabel(status: EvidenceStatus) {
  switch (status) {
    case "approved":
      return "Approved";
    case "rejected":
      return "Rejected";
    case "done":
      return "Submitted";
    default:
      return "Pending";
  }
}

function evidenceStatusTone(status: EvidenceStatus) {
  switch (status) {
    case "approved":
      return "good";
    case "rejected":
      return "warning";
    default:
      return "neutral";
  }
}

const styles = StyleSheet.create({
  description: {
    color: "#53666f",
    fontSize: 14,
    lineHeight: 20
  },
  emptyCard: {
    backgroundColor: "#ffffff",
    borderRadius: 12,
    padding: 16
  },
  errorText: {
    color: "#b42318",
    fontSize: 13,
    fontWeight: "700"
  },
  meta: {
    color: "#53666f",
    fontSize: 12,
    fontWeight: "700",
    marginTop: 6
  },
  queue: {
    gap: 12
  },
  reviewActions: {
    alignItems: "flex-end",
    gap: 8
  },
  reviewCard: {
    alignItems: "flex-start",
    backgroundColor: "#ffffff",
    borderRadius: 12,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    padding: 16
  },
  reviewText: {
    flex: 1
  },
  title: {
    color: "#172126",
    fontSize: 16,
    fontWeight: "800",
    marginBottom: 6
  }
});
