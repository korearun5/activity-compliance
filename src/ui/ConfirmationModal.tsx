import { Modal, Pressable, StyleSheet, Text, View } from "react-native";

type ConfirmationModalProps = {
  cancelLabel?: string;
  confirmLabel?: string;
  message: string;
  onCancel: () => void;
  onConfirm: () => void;
  title: string;
  visible: boolean;
};

export function ConfirmationModal({
  cancelLabel = "Cancel",
  confirmLabel = "Confirm",
  message,
  onCancel,
  onConfirm,
  title,
  visible
}: ConfirmationModalProps) {
  return (
    <Modal
      animationType="fade"
      onRequestClose={onCancel}
      transparent
      visible={visible}
    >
      <View style={styles.backdrop}>
        <View style={styles.card}>
          <Text style={styles.title}>{title}</Text>
          <Text style={styles.message}>{message}</Text>
          <View style={styles.actions}>
            <Pressable
              accessibilityRole="button"
              style={styles.secondaryButton}
              onPress={onCancel}
            >
              <Text style={styles.secondaryButtonText}>{cancelLabel}</Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              style={styles.primaryButton}
              onPress={onConfirm}
            >
              <Text style={styles.primaryButtonText}>{confirmLabel}</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  actions: {
    alignItems: "center",
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10,
    justifyContent: "flex-end",
    width: "100%"
  },
  backdrop: {
    alignItems: "center",
    backgroundColor: "rgba(17, 30, 36, 0.42)",
    flex: 1,
    justifyContent: "center",
    padding: 20
  },
  card: {
    alignItems: "flex-start",
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 14,
    maxWidth: 420,
    padding: 22,
    width: "100%"
  },
  message: {
    color: "#53666f",
    fontSize: 15,
    lineHeight: 22
  },
  primaryButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 42,
    minWidth: 96,
    paddingHorizontal: 14
  },
  primaryButtonText: {
    color: "#ffffff",
    fontSize: 14,
    fontWeight: "800"
  },
  secondaryButton: {
    alignItems: "center",
    borderColor: "#9fb4bf",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 42,
    minWidth: 88,
    paddingHorizontal: 14
  },
  secondaryButtonText: {
    color: "#53666f",
    fontSize: 14,
    fontWeight: "800"
  },
  title: {
    color: "#172126",
    fontSize: 22,
    fontWeight: "800"
  }
});
