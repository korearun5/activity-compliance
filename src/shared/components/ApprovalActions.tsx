import { Pressable, StyleSheet, Text, View } from "react-native";

type ApprovalActionsProps = {
  approveLabel?: string;
  isSubmitting?: boolean;
  onApprove: () => void;
  onReject: () => void;
  rejectLabel?: string;
  submittingLabel?: string;
};

export function ApprovalActions({
  approveLabel = "Approve",
  isSubmitting = false,
  onApprove,
  onReject,
  rejectLabel = "Reject",
  submittingLabel = "Saving..."
}: ApprovalActionsProps) {
  return (
    <View style={styles.actionRow}>
      <Pressable
        accessibilityRole="button"
        disabled={isSubmitting}
        style={({ pressed }) => [
          styles.approveButton,
          (pressed || isSubmitting) && styles.buttonPressed
        ]}
        onPress={onApprove}
      >
        <Text style={styles.approveButtonText}>
          {isSubmitting ? submittingLabel : approveLabel}
        </Text>
      </Pressable>
      <Pressable
        accessibilityRole="button"
        disabled={isSubmitting}
        style={({ pressed }) => [
          styles.rejectButton,
          (pressed || isSubmitting) && styles.buttonPressed
        ]}
        onPress={onReject}
      >
        <Text style={styles.rejectButtonText}>{rejectLabel}</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  actionRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    justifyContent: "flex-end"
  },
  approveButton: {
    alignItems: "center",
    backgroundColor: "#1f7a4d",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 34,
    minWidth: 86,
    paddingHorizontal: 12,
    paddingVertical: 8
  },
  approveButtonText: {
    color: "#ffffff",
    fontSize: 12,
    fontWeight: "800"
  },
  buttonPressed: {
    opacity: 0.72
  },
  rejectButton: {
    alignItems: "center",
    backgroundColor: "#fff1f0",
    borderColor: "#ffc9c4",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 34,
    minWidth: 74,
    paddingHorizontal: 12,
    paddingVertical: 8
  },
  rejectButtonText: {
    color: "#a13a31",
    fontSize: 12,
    fontWeight: "800"
  }
});
