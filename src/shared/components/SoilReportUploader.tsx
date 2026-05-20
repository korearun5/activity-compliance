import * as DocumentPicker from "expo-document-picker";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { UploadFileInput } from "../../core/api/uploadFile";

export type SoilReportField =
  | "bulkDensity"
  | "electricalConductivity"
  | "labName"
  | "nitrogen"
  | "notes"
  | "ph"
  | "phosphorus"
  | "potassium"
  | "reportFileName"
  | "reportUrl"
  | "soilOrganicCarbon"
  | "testDate"
  | "texture";

export type SoilReportValues = Partial<Record<SoilReportField, string>>;

type SoilReportUploaderProps = {
  endpointPrefix: string;
  error?: string;
  fields: SoilReportField[];
  hasUploadedFile?: boolean;
  isSubmitting: boolean;
  isUploadingFile?: boolean;
  module: "carbon" | "fpo";
  onChange: (field: SoilReportField, value: string) => void;
  onSubmit: () => void;
  onUploadFile?: (file: UploadFileInput) => Promise<void>;
  submitLabel?: string;
  submittingLabel?: string;
  title?: string;
  uploadLabel?: string;
  values: SoilReportValues;
};

const primaryFields: SoilReportField[] = [
  "testDate",
  "soilOrganicCarbon",
  "ph",
  "nitrogen",
  "phosphorus",
  "potassium"
];

const fieldConfig: Record<
  SoilReportField,
  { keyboardType?: "decimal-pad"; label: string; placeholder?: string }
> = {
  bulkDensity: { keyboardType: "decimal-pad", label: "Bulk density" },
  electricalConductivity: { keyboardType: "decimal-pad", label: "EC" },
  labName: { label: "Lab name" },
  nitrogen: { keyboardType: "decimal-pad", label: "Nitrogen" },
  notes: { label: "Notes" },
  ph: { keyboardType: "decimal-pad", label: "pH" },
  phosphorus: { keyboardType: "decimal-pad", label: "Phosphorus" },
  potassium: { keyboardType: "decimal-pad", label: "Potassium" },
  reportFileName: { label: "Report file name" },
  reportUrl: { label: "Report URL" },
  soilOrganicCarbon: { keyboardType: "decimal-pad", label: "SOC" },
  testDate: { label: "Test date", placeholder: "YYYY-MM-DD" },
  texture: { label: "Texture" }
};

export function SoilReportUploader({
  endpointPrefix,
  error,
  fields,
  hasUploadedFile = false,
  isSubmitting,
  isUploadingFile = false,
  module,
  onChange,
  onSubmit,
  onUploadFile,
  submitLabel = "Save soil profile",
  submittingLabel = "Saving...",
  title = "Soil report",
  uploadLabel,
  values
}: SoilReportUploaderProps) {
  const primary = fields.filter((field) => primaryFields.includes(field));
  const secondary = fields.filter((field) => !primaryFields.includes(field));

  async function handlePickReport() {
    if (!onUploadFile) {
      return;
    }

    const result = await DocumentPicker.getDocumentAsync({
      copyToCacheDirectory: true,
      multiple: false,
      type: ["application/pdf", "image/*"]
    });

    if (result.canceled) {
      return;
    }

    const asset = result.assets[0];
    await onUploadFile({
      name: asset.name,
      type: asset.mimeType,
      uri: asset.uri
    });
  }

  return (
    <View
      style={styles.formSection}
      testID={`${module}-soil-report-uploader-${safeTestId(endpointPrefix)}`}
    >
      <Text style={styles.sectionLabel}>{title}</Text>
      <View style={styles.formGrid}>
        {primary.map((field) => (
          <SoilReportFieldInput
            key={field}
            field={field}
            value={values[field] ?? ""}
            onChange={onChange}
          />
        ))}
      </View>

      {secondary.length ? (
        <View style={styles.formGrid}>
          {secondary.map((field) => (
            <SoilReportFieldInput
              key={field}
              field={field}
              value={values[field] ?? ""}
              onChange={onChange}
            />
          ))}
        </View>
      ) : null}

      {error ? <Text style={styles.formError}>{error}</Text> : null}

      <View style={styles.actionRow}>
        {onUploadFile ? (
          <Pressable
            accessibilityRole="button"
            disabled={isUploadingFile}
            style={[styles.secondaryButton, isUploadingFile && styles.disabledButton]}
            onPress={handlePickReport}
          >
            <Text style={styles.secondaryButtonText}>
              {isUploadingFile
                ? "Uploading..."
                : (uploadLabel ?? (hasUploadedFile ? "Replace report" : "Upload report"))}
            </Text>
          </Pressable>
        ) : null}
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

function SoilReportFieldInput({
  field,
  onChange,
  value
}: {
  field: SoilReportField;
  onChange: (field: SoilReportField, value: string) => void;
  value: string;
}) {
  const config = fieldConfig[field];

  return (
    <View style={styles.field}>
      <Text style={styles.fieldLabel}>{config.label}</Text>
      <TextInput
        autoCapitalize="none"
        autoCorrect={false}
        keyboardType={config.keyboardType}
        onChangeText={(nextValue) => onChange(field, nextValue)}
        placeholder={config.placeholder ?? config.label}
        placeholderTextColor="#8a99a1"
        style={styles.input}
        value={value}
      />
    </View>
  );
}

function safeTestId(value: string) {
  return value.replace(/[^a-zA-Z0-9_-]+/g, "-").replace(/^-|-$/g, "");
}

const styles = StyleSheet.create({
  actionRow: {
    alignItems: "flex-start",
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  disabledButton: {
    opacity: 0.6
  },
  field: {
    flex: 1,
    gap: 7,
    minWidth: 170
  },
  fieldLabel: {
    color: "#24343b",
    fontSize: 13,
    fontWeight: "800"
  },
  formError: {
    color: "#b42318",
    fontSize: 13,
    fontWeight: "700"
  },
  formGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
  },
  formSection: {
    gap: 12
  },
  input: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d8df",
    borderRadius: 8,
    borderWidth: 1,
    color: "#172126",
    fontSize: 15,
    minHeight: 46,
    paddingHorizontal: 12
  },
  primaryButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 42,
    minWidth: 120,
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
    minWidth: 120,
    paddingHorizontal: 14
  },
  secondaryButtonText: {
    color: "#1f6f73",
    fontSize: 13,
    fontWeight: "800"
  },
  sectionLabel: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800"
  }
});
