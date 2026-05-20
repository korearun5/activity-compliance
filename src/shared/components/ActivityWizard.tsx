import * as ImagePicker from "expo-image-picker";
import { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { CropCycle, CropStep, CropTemplate } from "../../data/agricultureConfig";
import {
  getBackendActivity,
  getBackendWorkflowTemplates,
  startBackendActivity,
  uploadBackendProof,
  WorkflowDomain
} from "../../data/workflowActivityStore";
import { StateCard } from "../../ui/StateCard";
import { StatusBadge } from "../../ui/StatusBadge";
import { EvidenceUploader } from "./EvidenceUploader";

type ActivityWizardProps = {
  domain: Extract<WorkflowDomain, "CARBON" | "FPO">;
  onActivityStarted?: () => void;
  participantName: string;
  participantRegion: string;
};

export function ActivityWizard({
  domain,
  onActivityStarted,
  participantName,
  participantRegion
}: ActivityWizardProps) {
  const module = domain === "CARBON" ? "carbon" : "fpo";
  const [activeActivity, setActiveActivity] = useState<CropCycle | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isStarting, setIsStarting] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [message, setMessage] = useState("");
  const [plotName, setPlotName] = useState("");
  const [proofError, setProofError] = useState("");
  const [proofNote, setProofNote] = useState("");
  const [proofPhotoUri, setProofPhotoUri] = useState<string | null>(null);
  const [selectedWorkflowId, setSelectedWorkflowId] = useState("");
  const [startDate, setStartDate] = useState(toInputDate(new Date()));
  const [startError, setStartError] = useState("");
  const [workflowTemplates, setWorkflowTemplates] = useState<CropTemplate[]>([]);

  useEffect(() => {
    async function loadWorkflows() {
      setIsLoading(true);
      setLoadError("");

      try {
        const templates = await getBackendWorkflowTemplates(domain);
        setWorkflowTemplates(templates);
        setSelectedWorkflowId((current) => current || templates[0]?.id || "");
      } catch (error) {
        setWorkflowTemplates([]);
        setLoadError(
          error instanceof Error ? error.message : "Unable to load activity workflows."
        );
      } finally {
        setIsLoading(false);
      }
    }

    loadWorkflows();
  }, [domain]);

  const selectedWorkflow = useMemo(
    () => workflowTemplates.find((template) => template.id === selectedWorkflowId),
    [selectedWorkflowId, workflowTemplates]
  );

  const nextEvidenceTask = useMemo(
    () => findNextEvidenceTask(activeActivity),
    [activeActivity]
  );

  async function handleStartActivity() {
    if (!selectedWorkflow?.id) {
      setStartError("Select an activity workflow.");
      return;
    }

    if (!plotName.trim()) {
      setStartError("Enter a plot or block name.");
      return;
    }

    if (Number.isNaN(new Date(startDate).getTime())) {
      setStartError("Enter a valid start date.");
      return;
    }

    setIsStarting(true);
    setMessage("");
    setProofError("");
    setStartError("");

    try {
      const activity = await startBackendActivity({
        locationName: participantRegion,
        startedOn: startDate,
        unitName: plotName.trim(),
        workflowDefinitionId: selectedWorkflow.id
      });

      setActiveActivity(activity);
      setMessage("Activity started. Upload evidence for the first required step.");
      onActivityStarted?.();
    } catch (error) {
      setStartError(
        error instanceof Error ? error.message : "Unable to start activity."
      );
    } finally {
      setIsStarting(false);
    }
  }

  async function handlePickPhoto() {
    setProofError("");

    const result = await ImagePicker.launchImageLibraryAsync({
      allowsEditing: true,
      aspect: [4, 3],
      quality: 0.75
    });

    if (!result.canceled) {
      setProofPhotoUri(result.assets[0].uri);
    }
  }

  async function handleSubmitEvidence() {
    if (!activeActivity || !nextEvidenceTask) {
      setProofError("Start an activity before uploading evidence.");
      return;
    }

    if (!proofPhotoUri) {
      setProofError("Attach an evidence photo before submitting.");
      return;
    }

    setIsUploading(true);
    setProofError("");
    setMessage("");

    try {
      await uploadBackendProof({
        activityId: activeActivity.id,
        activityTaskId: nextEvidenceTask.id,
        note: proofNote,
        photoUri: proofPhotoUri
      });
      const refreshedActivity = await getBackendActivity(activeActivity.id);

      setActiveActivity(refreshedActivity);
      setProofNote("");
      setProofPhotoUri(null);
      setMessage("Evidence submitted to the generic activity evidence queue.");
    } catch (error) {
      setProofError(
        error instanceof Error ? error.message : "Unable to upload evidence."
      );
    } finally {
      setIsUploading(false);
    }
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <View style={styles.headerText}>
          <Text style={styles.title}>
            {domain === "CARBON" ? "Start carbon activity" : "Start activity"}
          </Text>
          <Text style={styles.copy}>
            {participantName} - {participantRegion || "Unassigned location"}
          </Text>
        </View>
        <StatusBadge label={domain} tone="neutral" />
      </View>

      {loadError ? <StateCard message={loadError} title="Activities" tone="error" /> : null}
      {message ? <StateCard message={message} title="Activities" tone="success" /> : null}

      <View style={styles.formCard}>
        <Text style={styles.cardTitle}>1. Activity details</Text>
        <Text style={styles.inputLabel}>Workflow</Text>
        {isLoading ? (
          <Text style={styles.helperText}>Loading workflows...</Text>
        ) : workflowTemplates.length ? (
          <View style={styles.choiceRow}>
            {workflowTemplates.map((template) => {
              const isSelected = selectedWorkflowId === template.id;

              return (
                <Pressable
                  key={template.id ?? template.crop}
                  accessibilityRole="button"
                  style={[styles.choiceButton, isSelected && styles.choiceButtonActive]}
                  onPress={() => setSelectedWorkflowId(template.id ?? "")}
                >
                  <Text
                    style={[
                      styles.choiceButtonText,
                      isSelected && styles.choiceButtonTextActive
                    ]}
                  >
                    {template.crop}
                  </Text>
                  <Text style={styles.choiceButtonMeta}>
                    {template.durationDays} days
                  </Text>
                </Pressable>
              );
            })}
          </View>
        ) : (
          <StateCard
            message="No Carbon workflows are configured yet."
            tone="empty"
          />
        )}

        <Text style={styles.inputLabel}>Plot or block name</Text>
        <TextInput
          autoCapitalize="words"
          onChangeText={setPlotName}
          placeholder="Example: Block A"
          placeholderTextColor="#8a99a1"
          style={styles.textInput}
          value={plotName}
        />

        <Text style={styles.inputLabel}>Start date</Text>
        <TextInput
          autoCapitalize="none"
          onChangeText={setStartDate}
          placeholder="YYYY-MM-DD"
          placeholderTextColor="#8a99a1"
          style={styles.textInput}
          value={startDate}
        />

        {startError ? <Text style={styles.errorText}>{startError}</Text> : null}

        <View style={styles.actions}>
          <Pressable
            accessibilityRole="button"
            disabled={isStarting || isLoading}
            style={[styles.primaryButton, (isStarting || isLoading) && styles.disabledButton]}
            onPress={handleStartActivity}
          >
            <Text style={styles.primaryButtonText}>
              {isStarting ? "Starting..." : "Start activity"}
            </Text>
          </Pressable>
        </View>
      </View>

      {activeActivity && nextEvidenceTask ? (
        <EvidenceUploader
          description={`${activeActivity.crop} - ${activeActivity.plot} - ${nextEvidenceTask.title}`}
          error={proofError}
          fileLabel="Evidence photo"
          filePlaceholder="No photo selected"
          fileUri={proofPhotoUri}
          isSubmitting={isUploading}
          module={module}
          note={proofNote}
          notePlaceholder="Example: Compost applied across Block A using farmyard compost."
          pickLabel="Choose photo"
          submitLabel="Submit evidence"
          title="2. Upload evidence"
          onChangeNote={setProofNote}
          onPickFile={handlePickPhoto}
          onSubmit={handleSubmitEvidence}
        />
      ) : activeActivity ? (
        <StateCard
          message="This activity has no pending evidence task."
          title="Evidence"
          tone="empty"
        />
      ) : null}
    </View>
  );
}

function findNextEvidenceTask(activity: CropCycle | null): CropStep | null {
  if (!activity) {
    return null;
  }

  return (
    activity.steps.find((step) => step.status === "next") ??
    activity.steps.find((step) => step.status === "pending") ??
    activity.steps[0] ??
    null
  );
}

function toInputDate(date: Date) {
  return date.toISOString().slice(0, 10);
}

const styles = StyleSheet.create({
  actions: {
    alignItems: "flex-start",
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  cardTitle: {
    color: "#172126",
    fontSize: 17,
    fontWeight: "800"
  },
  choiceButton: {
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    minWidth: 140,
    paddingHorizontal: 12,
    paddingVertical: 10
  },
  choiceButtonActive: {
    backgroundColor: "#e8f3f2",
    borderColor: "#1f6f73"
  },
  choiceButtonMeta: {
    color: "#6d7f88",
    fontSize: 12,
    fontWeight: "700",
    marginTop: 4
  },
  choiceButtonText: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800"
  },
  choiceButtonTextActive: {
    color: "#1f6f73"
  },
  choiceRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  container: {
    gap: 14
  },
  copy: {
    color: "#53666f",
    fontSize: 14,
    lineHeight: 20
  },
  disabledButton: {
    opacity: 0.6
  },
  errorText: {
    color: "#b42318",
    fontSize: 13,
    fontWeight: "700"
  },
  formCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 12,
    padding: 16
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
  helperText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700"
  },
  inputLabel: {
    color: "#24343b",
    fontSize: 14,
    fontWeight: "800"
  },
  primaryButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 42,
    paddingHorizontal: 14
  },
  primaryButtonText: {
    color: "#ffffff",
    fontSize: 14,
    fontWeight: "800"
  },
  textInput: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    color: "#172126",
    fontSize: 15,
    minHeight: 48,
    paddingHorizontal: 12
  },
  title: {
    color: "#172126",
    fontSize: 18,
    fontWeight: "800",
    marginBottom: 4
  }
});
