import { Image, Pressable, StyleSheet, Text, TextInput, View } from "react-native";

type EvidenceUploaderProps = {
  cancelLabel?: string;
  description: string;
  error?: string;
  fileLabel?: string;
  filePlaceholder?: string;
  fileUri: string | null;
  isSubmitting?: boolean;
  module: "carbon" | "fpo";
  note: string;
  notePlaceholder?: string;
  onCancel?: () => void;
  onChangeNote: (note: string) => void;
  onPickFile: () => void;
  onSubmit: () => void;
  pickLabel?: string;
  submitLabel?: string;
  submittingLabel?: string;
  title?: string;
};

export function EvidenceUploader({
  cancelLabel = "Cancel",
  description,
  error,
  fileLabel = "Evidence file",
  filePlaceholder = "No file selected",
  fileUri,
  isSubmitting = false,
  module,
  note,
  notePlaceholder = "Add a short note for the reviewer.",
  onCancel,
  onChangeNote,
  onPickFile,
  onSubmit,
  pickLabel = "Choose file",
  submitLabel = "Submit evidence",
  submittingLabel = "Uploading...",
  title = "Submit evidence"
}: EvidenceUploaderProps) {
  const isImage = Boolean(fileUri);

  return (
    <View style={styles.formCard} testID={`${module}-evidence-uploader`}>
      <View style={styles.header}>
        <View style={styles.headerText}>
          <Text style={styles.title}>{title}</Text>
          <Text style={styles.description}>{description}</Text>
        </View>
        {onCancel ? (
          <Pressable accessibilityRole="button" style={styles.closeButton} onPress={onCancel}>
            <Text style={styles.closeButtonText}>{cancelLabel}</Text>
          </Pressable>
        ) : null}
      </View>

      <Text style={styles.label}>Action note</Text>
      <TextInput
        multiline
        numberOfLines={4}
        onChangeText={onChangeNote}
        placeholder={notePlaceholder}
        placeholderTextColor="#8a99a1"
        style={styles.noteInput}
        textAlignVertical="top"
        value={note}
      />

      <Text style={styles.label}>{fileLabel}</Text>
      {isImage ? (
        <Image source={{ uri: fileUri ?? undefined }} style={styles.filePreview} />
      ) : (
        <View style={styles.filePlaceholder}>
          <Text style={styles.filePlaceholderText}>{filePlaceholder}</Text>
        </View>
      )}

      {error ? <Text style={styles.errorText}>{error}</Text> : null}

      <View style={styles.actions}>
        <Pressable
          accessibilityRole="button"
          disabled={isSubmitting}
          style={[styles.secondaryButton, isSubmitting && styles.disabledButton]}
          onPress={onPickFile}
        >
          <Text style={styles.secondaryButtonText}>{pickLabel}</Text>
        </Pressable>
        <Pressable
          accessibilityRole="button"
          disabled={isSubmitting}
          style={[styles.primaryButton, isSubmitting && styles.disabledButton]}
          onPress={onSubmit}
        >
          <Text style={styles.primaryButtonText}>
            {isSubmitting ? submittingLabel : submitLabel}
          </Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  actions: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  closeButton: {
    borderColor: "#c9d8df",
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 12,
    paddingVertical: 8
  },
  closeButtonText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  description: {
    color: "#53666f",
    fontSize: 13,
    lineHeight: 19
  },
  disabledButton: {
    opacity: 0.6
  },
  errorText: {
    color: "#b42318",
    fontSize: 13,
    fontWeight: "700"
  },
  filePlaceholder: {
    alignItems: "center",
    backgroundColor: "#f7fafb",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 120,
    padding: 14
  },
  filePlaceholderText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700"
  },
  filePreview: {
    backgroundColor: "#f7fafb",
    borderRadius: 8,
    height: 180,
    width: "100%"
  },
  formCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 12,
    padding: 14
  },
  header: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  headerText: {
    flex: 1
  },
  label: {
    color: "#24343b",
    fontSize: 13,
    fontWeight: "800"
  },
  noteInput: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d8df",
    borderRadius: 8,
    borderWidth: 1,
    color: "#172126",
    fontSize: 14,
    minHeight: 94,
    padding: 12
  },
  primaryButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 42,
    minWidth: 128,
    paddingHorizontal: 14
  },
  primaryButtonText: {
    color: "#ffffff",
    fontSize: 13,
    fontWeight: "800"
  },
  secondaryButton: {
    alignItems: "center",
    backgroundColor: "#e8f3f2",
    borderColor: "#b9d8d6",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 42,
    minWidth: 112,
    paddingHorizontal: 14
  },
  secondaryButtonText: {
    color: "#1f6f73",
    fontSize: 13,
    fontWeight: "800"
  },
  title: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800",
    marginBottom: 3
  }
});
