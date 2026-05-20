import { ReactNode, useEffect, useState } from "react";
import { StyleSheet, Text, View } from "react-native";

import { UploadFileInput } from "../../core/api/uploadFile";
import { StatusBadge } from "../../ui/StatusBadge";
import {
  SoilReportField,
  SoilReportUploader,
  SoilReportValues
} from "./SoilReportUploader";
import { SoilProfileStatusTone } from "./SoilProfileList";

type SoilManualEntryFormProps<TInput, TResult> = {
  badgeLabel?: string;
  badgeTone?: SoilProfileStatusTone;
  buildInput: (values: SoilReportValues) => TInput;
  childrenBeforeFields?: ReactNode;
  clearOnSuccess?: boolean;
  description?: string;
  endpointPrefix: string;
  fields: SoilReportField[];
  formTitle?: string;
  hasUploadedFile?: boolean;
  initialValues?: SoilReportValues;
  isSubmitting: boolean;
  isUploadingFile?: boolean;
  module: "carbon" | "fpo";
  onSubmit: (input: TInput) => Promise<TResult>;
  onSubmitted?: (result: TResult, values: SoilReportValues) => void;
  onUploadFile?: (file: UploadFileInput, input: TInput) => Promise<void>;
  submitLabel?: string;
  submittingLabel?: string;
  title: string;
  uploadLabel?: string;
  validate?: (input: TInput, values: SoilReportValues) => string;
};

const emptySoilValues: SoilReportValues = {};

export function SoilManualEntryForm<TInput, TResult = unknown>({
  badgeLabel,
  badgeTone = "neutral",
  buildInput,
  childrenBeforeFields,
  clearOnSuccess = false,
  description,
  endpointPrefix,
  fields,
  formTitle,
  hasUploadedFile = false,
  initialValues = emptySoilValues,
  isSubmitting,
  isUploadingFile = false,
  module,
  onSubmit,
  onSubmitted,
  onUploadFile,
  submitLabel,
  submittingLabel,
  title,
  uploadLabel,
  validate
}: SoilManualEntryFormProps<TInput, TResult>) {
  const [values, setValues] = useState<SoilReportValues>(initialValues);
  const [localError, setLocalError] = useState("");

  useEffect(() => {
    setValues(initialValues);
    setLocalError("");
  }, [initialValues]);

  function handleChange(field: SoilReportField, value: string) {
    setValues((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit() {
    const input = buildInput(values);
    const validationError = validate?.(input, values) ?? "";

    if (validationError) {
      setLocalError(validationError);
      return;
    }

    setLocalError("");
    const result = await onSubmit(input);

    if (clearOnSuccess && result) {
      setValues({});
    }

    onSubmitted?.(result, values);
  }

  async function handleUploadFile(file: UploadFileInput) {
    if (!onUploadFile) {
      return;
    }

    const input = buildInput(values);
    const validationError = validate?.(input, values) ?? "";

    if (validationError) {
      setLocalError(validationError);
      return;
    }

    setLocalError("");
    await onUploadFile(file, input);
  }

  return (
    <View style={styles.card}>
      <View style={styles.header}>
        <View style={styles.headerText}>
          <Text style={styles.title}>{title}</Text>
          {description ? <Text style={styles.description}>{description}</Text> : null}
        </View>
        {badgeLabel ? <StatusBadge label={badgeLabel} tone={badgeTone} /> : null}
      </View>

      {childrenBeforeFields}

      <SoilReportUploader
        endpointPrefix={endpointPrefix}
        error={localError}
        fields={fields}
        hasUploadedFile={hasUploadedFile}
        isSubmitting={isSubmitting}
        isUploadingFile={isUploadingFile}
        module={module}
        submitLabel={submitLabel}
        submittingLabel={submittingLabel}
        title={formTitle ?? title}
        uploadLabel={uploadLabel}
        values={values}
        onChange={handleChange}
        onSubmit={handleSubmit}
        onUploadFile={onUploadFile ? handleUploadFile : undefined}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 12,
    padding: 16
  },
  description: {
    color: "#53666f",
    fontSize: 14,
    lineHeight: 20
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
  title: {
    color: "#172126",
    fontSize: 18,
    fontWeight: "800"
  }
});
