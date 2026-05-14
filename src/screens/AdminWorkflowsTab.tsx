import { useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { FpoMember } from "../data/fpoMemberStore";
import {
  BackendWorkflow,
  BackendWorkflowStatus,
  CreateBackendWorkflowInput,
  StartBackendActivityInput
} from "../data/workflowActivityStore";
import { StatusBadge } from "../ui/StatusBadge";

type AdminWorkflowsTabProps = {
  activityStartError: string;
  canManageDefinitions: boolean;
  canUseBackend: boolean;
  createWorkflowError: string;
  isCreatingWorkflow: boolean;
  onCreateWorkflow: (input: CreateBackendWorkflowInput) => Promise<boolean>;
  onStartActivity: (input: StartBackendActivityInput) => Promise<boolean>;
  onUpdateWorkflowStatus: (
    workflowId: string,
    status: BackendWorkflowStatus
  ) => Promise<void>;
  participants: FpoMember[];
  startingActivity: boolean;
  updatingWorkflowId: string | null;
  workflows: BackendWorkflow[];
};

type WorkflowTaskDraft = {
  code: string;
  evidenceRequired: boolean;
  localId: string;
  offsetDays: string;
  title: string;
};

export function AdminWorkflowsTab({
  activityStartError,
  canManageDefinitions,
  canUseBackend,
  createWorkflowError,
  isCreatingWorkflow,
  onCreateWorkflow,
  onStartActivity,
  onUpdateWorkflowStatus,
  participants,
  startingActivity,
  updatingWorkflowId,
  workflows
}: AdminWorkflowsTabProps) {
  const activeWorkflows = useMemo(
    () => workflows.filter((workflow) => workflow.status === "ACTIVE"),
    [workflows]
  );

  return (
    <View style={styles.section}>
      {!canUseBackend ? (
        <View style={styles.warningCard}>
          <Text style={styles.warningText}>
            Backend session is required for workflow definitions and assigned activity
            timelines.
          </Text>
        </View>
      ) : null}

      {canManageDefinitions ? (
        <CreateWorkflowForm
          error={createWorkflowError}
          isSubmitting={isCreatingWorkflow}
          onSubmit={onCreateWorkflow}
        />
      ) : null}

      <StartActivityForm
        activeWorkflows={activeWorkflows}
        error={activityStartError}
        isSubmitting={startingActivity}
        onSubmit={onStartActivity}
        participants={participants}
      />

      <Text style={styles.sectionTitle}>Workflow definitions</Text>
      {workflows.length ? (
        workflows.map((workflow) => (
          <WorkflowDefinitionCard
            canManageDefinitions={canManageDefinitions}
            key={workflow.id}
            onUpdateWorkflowStatus={onUpdateWorkflowStatus}
            updatingWorkflowId={updatingWorkflowId}
            workflow={workflow}
          />
        ))
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.cardDescription}>
            No workflow definitions are available yet.
          </Text>
        </View>
      )}
    </View>
  );
}

function CreateWorkflowForm({
  error,
  isSubmitting,
  onSubmit
}: {
  error: string;
  isSubmitting: boolean;
  onSubmit: (input: CreateBackendWorkflowInput) => Promise<boolean>;
}) {
  const [code, setCode] = useState("");
  const [domainKey, setDomainKey] = useState("");
  const [durationDays, setDurationDays] = useState("90");
  const [localError, setLocalError] = useState("");
  const [name, setName] = useState("");
  const [status, setStatus] = useState<BackendWorkflowStatus>("ACTIVE");
  const [tasks, setTasks] = useState<WorkflowTaskDraft[]>([emptyTaskDraft("task-1")]);
  const [version, setVersion] = useState("1");

  async function handleSubmit() {
    const parsedDurationDays = Number(durationDays);
    const parsedVersion = Number(version);
    const parsedTasks = tasks.map((task, index) => ({
      code: task.code.trim(),
      evidenceRequired: task.evidenceRequired,
      offsetDays: Number(task.offsetDays),
      sequenceNumber: index + 1,
      title: task.title.trim()
    }));

    if (!code.trim() || !name.trim()) {
      setLocalError("Enter workflow code and name.");
      return;
    }

    if (!isPositiveInteger(parsedDurationDays) || !isPositiveInteger(parsedVersion)) {
      setLocalError("Duration days and version must be positive whole numbers.");
      return;
    }

    if (
      parsedTasks.some(
        (task) =>
          !task.code ||
          !task.title ||
          !isNonNegativeInteger(task.offsetDays) ||
          !isWorkflowCode(task.code)
      )
    ) {
      setLocalError("Enter valid task codes, titles, and offset days.");
      return;
    }

    if (!isWorkflowCode(code.trim())) {
      setLocalError("Workflow code must use letters, numbers, and hyphens.");
      return;
    }

    setLocalError("");
    const created = await onSubmit({
      code: code.trim(),
      domainKey: domainKey.trim() || null,
      durationDays: parsedDurationDays,
      name: name.trim(),
      status,
      tasks: parsedTasks,
      version: parsedVersion
    });

    if (created) {
      setCode("");
      setDomainKey("");
      setDurationDays("90");
      setName("");
      setStatus("ACTIVE");
      setTasks([emptyTaskDraft(`task-${Date.now()}`)]);
      setVersion("1");
    }
  }

  function updateTask(localId: string, patch: Partial<WorkflowTaskDraft>) {
    setTasks((currentTasks) =>
      currentTasks.map((task) =>
        task.localId === localId ? { ...task, ...patch } : task
      )
    );
  }

  return (
    <View style={styles.managementCard}>
      <View>
        <Text style={styles.cardTitle}>Create workflow definition</Text>
        <Text style={styles.cardDescription}>
          Define the reusable activity sequence and due-date offsets.
        </Text>
      </View>

      <View style={styles.formGrid}>
        <WorkflowField label="Workflow code" value={code} onChange={setCode} />
        <WorkflowField label="Name" value={name} onChange={setName} />
        <WorkflowField
          keyboardType="numeric"
          label="Duration days"
          value={durationDays}
          onChange={setDurationDays}
        />
        <WorkflowField
          keyboardType="numeric"
          label="Version"
          value={version}
          onChange={setVersion}
        />
        <WorkflowField label="Domain key" value={domainKey} onChange={setDomainKey} />
      </View>

      <SegmentedStatusControl status={status} onChange={setStatus} />

      <Text style={styles.subsectionTitle}>Task timeline</Text>
      {tasks.map((task, index) => (
        <View key={task.localId} style={styles.taskRow}>
          <View style={styles.taskSequence}>
            <Text style={styles.taskSequenceText}>{index + 1}</Text>
          </View>
          <View style={styles.taskFields}>
            <WorkflowField
              label="Task code"
              value={task.code}
              onChange={(value) => updateTask(task.localId, { code: value })}
            />
            <WorkflowField
              label="Task title"
              value={task.title}
              onChange={(value) => updateTask(task.localId, { title: value })}
            />
            <WorkflowField
              keyboardType="numeric"
              label="Offset days"
              value={task.offsetDays}
              onChange={(value) => updateTask(task.localId, { offsetDays: value })}
            />
          </View>
          <View style={styles.taskActions}>
            <Pressable
              accessibilityRole="button"
              style={[
                styles.toggleButton,
                task.evidenceRequired && styles.toggleButtonActive
              ]}
              onPress={() =>
                updateTask(task.localId, {
                  evidenceRequired: !task.evidenceRequired
                })
              }
            >
              <Text
                style={[
                  styles.toggleButtonText,
                  task.evidenceRequired && styles.toggleButtonTextActive
                ]}
              >
                Evidence
              </Text>
            </Pressable>
            {tasks.length > 1 ? (
              <Pressable
                accessibilityRole="button"
                style={styles.removeButton}
                onPress={() =>
                  setTasks((currentTasks) =>
                    currentTasks.filter((item) => item.localId !== task.localId)
                  )
                }
              >
                <Text style={styles.removeButtonText}>Remove</Text>
              </Pressable>
            ) : null}
          </View>
        </View>
      ))}

      <View style={styles.formActions}>
        <Pressable
          accessibilityRole="button"
          style={styles.secondaryButton}
          onPress={() =>
            setTasks((currentTasks) => [
              ...currentTasks,
              emptyTaskDraft(`task-${Date.now()}`)
            ])
          }
        >
          <Text style={styles.secondaryButtonText}>Add task</Text>
        </Pressable>
        <Pressable
          accessibilityRole="button"
          disabled={isSubmitting}
          style={[styles.primaryButton, isSubmitting && styles.disabledButton]}
          onPress={handleSubmit}
        >
          <Text style={styles.primaryButtonText}>
            {isSubmitting ? "Creating..." : "Create workflow"}
          </Text>
        </Pressable>
      </View>

      {localError || error ? (
        <Text style={styles.formError}>{localError || error}</Text>
      ) : null}
    </View>
  );
}

function StartActivityForm({
  activeWorkflows,
  error,
  isSubmitting,
  onSubmit,
  participants
}: {
  activeWorkflows: BackendWorkflow[];
  error: string;
  isSubmitting: boolean;
  onSubmit: (input: StartBackendActivityInput) => Promise<boolean>;
  participants: FpoMember[];
}) {
  const [localError, setLocalError] = useState("");
  const [locationName, setLocationName] = useState("");
  const [participantUserId, setParticipantUserId] = useState("");
  const [startedOn, setStartedOn] = useState(toInputDate(new Date()));
  const [unitName, setUnitName] = useState("");
  const [workflowDefinitionId, setWorkflowDefinitionId] = useState("");

  async function handleSubmit() {
    const participant = participants.find((item) => item.id === participantUserId);

    if (!workflowDefinitionId || !participantUserId || !participant) {
      setLocalError("Select an active workflow and participant.");
      return;
    }

    if (!unitName.trim() || !startedOn.trim()) {
      setLocalError("Enter unit name and start date.");
      return;
    }

    setLocalError("");
    const started = await onSubmit({
      locationName: locationName.trim() || participant.locationName,
      participantUserId,
      startedOn: startedOn.trim(),
      unitName: unitName.trim(),
      workflowDefinitionId
    });

    if (started) {
      setLocationName("");
      setParticipantUserId("");
      setStartedOn(toInputDate(new Date()));
      setUnitName("");
      setWorkflowDefinitionId("");
    }
  }

  return (
    <View style={styles.managementCard}>
      <View>
        <Text style={styles.cardTitle}>Assign activity timeline</Text>
        <Text style={styles.cardDescription}>
          Start an activity for a participant from an active workflow definition.
        </Text>
      </View>

      <Text style={styles.formLabel}>Workflow</Text>
      <View style={styles.choiceRow}>
        {activeWorkflows.map((workflow) => (
          <Pressable
            accessibilityRole="button"
            key={workflow.id}
            style={[
              styles.choiceButton,
              workflowDefinitionId === workflow.id && styles.choiceButtonActive
            ]}
            onPress={() => setWorkflowDefinitionId(workflow.id)}
          >
            <Text
              style={[
                styles.choiceButtonText,
                workflowDefinitionId === workflow.id && styles.choiceButtonTextActive
              ]}
            >
              {workflow.name}
            </Text>
            <Text style={styles.choiceButtonMeta}>
              {workflow.tasks.length} tasks, {workflow.durationDays} days
            </Text>
          </Pressable>
        ))}
      </View>

      <Text style={styles.formLabel}>Participant</Text>
      <View style={styles.choiceRow}>
        {participants
          .filter((participant) => participant.id && participant.status === "Active")
          .map((participant) => (
            <Pressable
              accessibilityRole="button"
              key={participant.id}
              style={[
                styles.choiceButton,
                participantUserId === participant.id && styles.choiceButtonActive
              ]}
              onPress={() => {
                setParticipantUserId(participant.id ?? "");
                setLocationName(participant.locationName);
              }}
            >
              <Text
                style={[
                  styles.choiceButtonText,
                  participantUserId === participant.id && styles.choiceButtonTextActive
                ]}
              >
                {participant.name}
              </Text>
              <Text style={styles.choiceButtonMeta}>
                {participant.locationName} - {participant.siteName}
              </Text>
            </Pressable>
          ))}
      </View>

      <View style={styles.formGrid}>
        <WorkflowField label="Unit name" value={unitName} onChange={setUnitName} />
        <WorkflowField
          label="Location"
          value={locationName}
          onChange={setLocationName}
        />
        <WorkflowField label="Start date" value={startedOn} onChange={setStartedOn} />
      </View>

      {localError || error ? (
        <Text style={styles.formError}>{localError || error}</Text>
      ) : null}

      <View style={styles.formActions}>
        <Pressable
          accessibilityRole="button"
          disabled={isSubmitting}
          style={[styles.primaryButton, isSubmitting && styles.disabledButton]}
          onPress={handleSubmit}
        >
          <Text style={styles.primaryButtonText}>
            {isSubmitting ? "Starting..." : "Start activity"}
          </Text>
        </Pressable>
      </View>
    </View>
  );
}

function WorkflowDefinitionCard({
  canManageDefinitions,
  onUpdateWorkflowStatus,
  updatingWorkflowId,
  workflow
}: {
  canManageDefinitions: boolean;
  onUpdateWorkflowStatus: (
    workflowId: string,
    status: BackendWorkflowStatus
  ) => Promise<void>;
  updatingWorkflowId: string | null;
  workflow: BackendWorkflow;
}) {
  const isUpdating = updatingWorkflowId === workflow.id;

  return (
    <View style={styles.workflowCard}>
      <View style={styles.cardHeader}>
        <View style={styles.cardHeaderText}>
          <Text style={styles.cardTitle}>{workflow.name}</Text>
          <Text style={styles.cardDescription}>
            {workflow.code} v{workflow.version} - {workflow.durationDays} days
          </Text>
        </View>
        <StatusBadge label={workflow.status} tone={statusTone(workflow.status)} />
      </View>

      <View style={styles.taskList}>
        {workflow.tasks
          .slice()
          .sort((left, right) => left.sequenceNumber - right.sequenceNumber)
          .map((task) => (
            <View key={task.id} style={styles.timelineRow}>
              <Text style={styles.timelineSequence}>{task.sequenceNumber}</Text>
              <View style={styles.timelineText}>
                <Text style={styles.timelineTitle}>{task.title}</Text>
                <Text style={styles.timelineMeta}>
                  Day {task.offsetDays} -{" "}
                  {task.evidenceRequired ? "Evidence" : "No evidence"}
                </Text>
              </View>
            </View>
          ))}
      </View>

      {canManageDefinitions ? (
        <View style={styles.statusActions}>
          {workflow.status !== "ACTIVE" ? (
            <Pressable
              accessibilityRole="button"
              disabled={isUpdating}
              style={[styles.secondaryButton, isUpdating && styles.disabledButton]}
              onPress={() => onUpdateWorkflowStatus(workflow.id, "ACTIVE")}
            >
              <Text style={styles.secondaryButtonText}>
                {isUpdating ? "Saving..." : "Publish"}
              </Text>
            </Pressable>
          ) : null}
          {workflow.status !== "DRAFT" ? (
            <Pressable
              accessibilityRole="button"
              disabled={isUpdating}
              style={[styles.secondaryButton, isUpdating && styles.disabledButton]}
              onPress={() => onUpdateWorkflowStatus(workflow.id, "DRAFT")}
            >
              <Text style={styles.secondaryButtonText}>Move to draft</Text>
            </Pressable>
          ) : null}
          {workflow.status !== "ARCHIVED" ? (
            <Pressable
              accessibilityRole="button"
              disabled={isUpdating}
              style={[styles.archiveButton, isUpdating && styles.disabledButton]}
              onPress={() => onUpdateWorkflowStatus(workflow.id, "ARCHIVED")}
            >
              <Text style={styles.archiveButtonText}>Archive</Text>
            </Pressable>
          ) : null}
        </View>
      ) : null}
    </View>
  );
}

function WorkflowField({
  keyboardType,
  label,
  onChange,
  value
}: {
  keyboardType?: "default" | "numeric";
  label: string;
  onChange: (value: string) => void;
  value: string;
}) {
  return (
    <View style={styles.formField}>
      <Text style={styles.formLabel}>{label}</Text>
      <TextInput
        autoCapitalize="none"
        autoCorrect={false}
        keyboardType={keyboardType}
        onChangeText={onChange}
        style={styles.formInput}
        value={value}
      />
    </View>
  );
}

function SegmentedStatusControl({
  onChange,
  status
}: {
  onChange: (status: BackendWorkflowStatus) => void;
  status: BackendWorkflowStatus;
}) {
  const options: BackendWorkflowStatus[] = ["ACTIVE", "DRAFT", "ARCHIVED"];

  return (
    <View style={styles.segmentRow}>
      {options.map((option) => (
        <Pressable
          accessibilityRole="button"
          key={option}
          style={[
            styles.segmentButton,
            status === option && styles.segmentButtonActive
          ]}
          onPress={() => onChange(option)}
        >
          <Text
            style={[
              styles.segmentButtonText,
              status === option && styles.segmentButtonTextActive
            ]}
          >
            {option}
          </Text>
        </Pressable>
      ))}
    </View>
  );
}

function emptyTaskDraft(localId: string): WorkflowTaskDraft {
  return {
    code: "",
    evidenceRequired: true,
    localId,
    offsetDays: "0",
    title: ""
  };
}

function isWorkflowCode(value: string) {
  return /^[A-Za-z0-9][A-Za-z0-9-]*$/.test(value);
}

function isPositiveInteger(value: number) {
  return Number.isInteger(value) && value > 0;
}

function isNonNegativeInteger(value: number) {
  return Number.isInteger(value) && value >= 0;
}

function statusTone(status: BackendWorkflowStatus) {
  switch (status) {
    case "ACTIVE":
      return "good";
    case "ARCHIVED":
      return "neutral";
    default:
      return "warning";
  }
}

function toInputDate(date: Date) {
  return date.toISOString().slice(0, 10);
}

const styles = StyleSheet.create({
  section: {
    gap: 14
  },
  sectionTitle: {
    color: "#172126",
    fontSize: 18,
    fontWeight: "800",
    marginTop: 4
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
  managementCard: {
    backgroundColor: "#ffffff",
    borderColor: "#cfe0df",
    borderRadius: 8,
    borderWidth: 1,
    gap: 14,
    padding: 16
  },
  workflowCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 14,
    padding: 16
  },
  emptyCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    padding: 16
  },
  cardHeader: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  cardHeaderText: {
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
  subsectionTitle: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  },
  formGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
  },
  formField: {
    flex: 1,
    gap: 7,
    minWidth: 170
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
  formError: {
    color: "#b42318",
    fontSize: 13,
    fontWeight: "700"
  },
  formActions: {
    alignItems: "flex-start",
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  primaryButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 44,
    minWidth: 136,
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
    minWidth: 112,
    paddingHorizontal: 12
  },
  secondaryButtonText: {
    color: "#1f6f73",
    fontSize: 13,
    fontWeight: "800"
  },
  archiveButton: {
    alignItems: "center",
    borderColor: "#b42318",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 40,
    minWidth: 92,
    paddingHorizontal: 12
  },
  archiveButtonText: {
    color: "#b42318",
    fontSize: 13,
    fontWeight: "800"
  },
  disabledButton: {
    opacity: 0.6
  },
  segmentRow: {
    backgroundColor: "#e8eef2",
    borderRadius: 8,
    flexDirection: "row",
    gap: 4,
    padding: 4
  },
  segmentButton: {
    alignItems: "center",
    borderRadius: 6,
    flex: 1,
    minHeight: 38,
    justifyContent: "center"
  },
  segmentButtonActive: {
    backgroundColor: "#ffffff"
  },
  segmentButtonText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  segmentButtonTextActive: {
    color: "#172126"
  },
  taskRow: {
    alignItems: "flex-start",
    borderTopColor: "#e6eef2",
    borderTopWidth: 1,
    flexDirection: "row",
    gap: 12,
    paddingTop: 12
  },
  taskSequence: {
    alignItems: "center",
    backgroundColor: "#e8f3f2",
    borderRadius: 8,
    height: 36,
    justifyContent: "center",
    width: 36
  },
  taskSequenceText: {
    color: "#1f6f73",
    fontSize: 14,
    fontWeight: "800"
  },
  taskFields: {
    flex: 1,
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
  },
  taskActions: {
    alignItems: "flex-start",
    gap: 8
  },
  toggleButton: {
    alignItems: "center",
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 38,
    minWidth: 88,
    paddingHorizontal: 10
  },
  toggleButtonActive: {
    backgroundColor: "#e8f3f2",
    borderColor: "#1f6f73"
  },
  toggleButtonText: {
    color: "#53666f",
    fontSize: 12,
    fontWeight: "800"
  },
  toggleButtonTextActive: {
    color: "#1f6f73"
  },
  removeButton: {
    alignItems: "center",
    justifyContent: "center",
    minHeight: 34,
    minWidth: 76
  },
  removeButtonText: {
    color: "#b42318",
    fontSize: 12,
    fontWeight: "800"
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
    minWidth: 160,
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
  taskList: {
    gap: 8
  },
  timelineRow: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 10
  },
  timelineSequence: {
    color: "#1f6f73",
    fontSize: 13,
    fontWeight: "800",
    width: 24
  },
  timelineText: {
    flex: 1
  },
  timelineTitle: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800"
  },
  timelineMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    marginTop: 2
  },
  statusActions: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  }
});
