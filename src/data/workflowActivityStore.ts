import AsyncStorage from "@react-native-async-storage/async-storage";

import { apiClient } from "../core/api/client";
import { PageResponse } from "../core/api/contracts";
import { endpoints } from "../core/api/endpoints";
import { appendUploadFile } from "../core/api/uploadFile";
import {
  ActivityStatus,
  ActivityTask,
  EvidenceStatus,
  TaskStatus
} from "../core/model/types";
import { storageKeys } from "../core/storage/storageKeys";
import { CropCycle, CropTemplate, ProofSubmission } from "./agricultureConfig";

export type BackendWorkflowStatus = "ACTIVE" | "ARCHIVED" | "DRAFT";

export type WorkflowDomain = "CARBON" | "COMMON" | "FPO" | "agriculture";

type BackendTaskStatus = "DONE" | "NEXT" | "PENDING" | "SKIPPED";

type BackendActivityStatus = "CANCELLED" | "COMPLETED" | "RUNNING";

type BackendEvidenceStatus = "APPROVED" | "PENDING_REVIEW" | "REJECTED" | "SUBMITTED";

export type BackendWorkflowTask = {
  code: string;
  evidenceRequired: boolean;
  id: string;
  offsetDays: number;
  sequenceNumber: number;
  title: string;
};

export type BackendWorkflow = {
  code: string;
  createdAt: string;
  domainKey: string | null;
  durationDays: number;
  id: string;
  name: string;
  status: BackendWorkflowStatus;
  tasks: BackendWorkflowTask[];
  tenantId: string;
  updatedAt: string;
  version: number;
};

type BackendActivityTask = {
  code: string;
  completedAt: string | null;
  dueOn: string;
  evidenceRequired: boolean;
  id: string;
  sequenceNumber: number;
  status: BackendTaskStatus;
  title: string;
  workflowTaskId: string;
};

type BackendActivity = {
  completedAt: string | null;
  createdAt: string;
  expectedCompletion: string;
  id: string;
  locationName: string | null;
  participantName: string | null;
  participantUserId: string | null;
  progressPercent: number;
  startedOn: string;
  status: BackendActivityStatus;
  tasks: BackendActivityTask[];
  tenantId: string;
  unitName: string;
  updatedAt: string;
  workflowDefinitionId: string;
  workflowDomainKey: string | null;
  workflowName: string;
};

type BackendEvidence = {
  activityId: string;
  activityTaskId: string;
  contentType: string;
  id: string;
  locationName: string | null;
  note: string | null;
  originalFilename: string;
  participantName: string | null;
  participantUserId: string | null;
  reviewedAt: string | null;
  reviewedByUserId: string | null;
  sizeBytes: number;
  status: BackendEvidenceStatus;
  storageKey: string;
  submittedAt: string;
  taskCode: string;
  taskTitle: string;
  tenantId: string;
  unitName: string;
  workflowDomainKey: string | null;
  workflowName: string;
};

export type StartBackendActivityInput = {
  locationName?: string;
  participantUserId?: string;
  startedOn: string;
  unitName: string;
  workflowDefinitionId: string;
};

type UploadBackendEvidenceInput = {
  activityId: string;
  activityTaskId: string;
  note?: string;
  photoUri: string;
};

export type BackendWorkflowTaskInput = {
  code: string;
  evidenceRequired: boolean;
  offsetDays: number;
  sequenceNumber: number;
  title: string;
};

export type CreateBackendWorkflowInput = {
  code: string;
  domainKey?: string | null;
  durationDays: number;
  name: string;
  status: BackendWorkflowStatus;
  tasks: BackendWorkflowTaskInput[];
  version: number;
};

type ReviewBackendEvidenceInput = {
  evidenceId: string;
  status: Extract<BackendEvidenceStatus, "APPROVED" | "REJECTED">;
};

export async function hasBackendSession() {
  return Boolean(await AsyncStorage.getItem(storageKeys.auth.accessToken));
}

export async function getBackendWorkflowTemplates(domain?: WorkflowDomain) {
  const params = new URLSearchParams({
    size: "100",
    sort: "createdAt,desc",
    status: "ACTIVE"
  });
  if (domain) {
    params.set("domain", domain);
  }

  const response = await apiClient.get<PageResponse<BackendWorkflow>>(
    `${endpoints.workflows.list}?${params.toString()}`
  );

  return response.content.map(toCropTemplate);
}

export async function getBackendWorkflowDefinitions(
  status?: BackendWorkflowStatus,
  domain?: WorkflowDomain
) {
  const params = new URLSearchParams();
  if (status) {
    params.set("status", status);
  }
  if (domain) {
    params.set("domain", domain);
  }
  const path = params.toString()
    ? `${endpoints.workflows.list}?${params.toString()}`
    : endpoints.workflows.list;
  const response = await apiClient.getPaginated<PageResponse<BackendWorkflow>>(path, {
    size: 100,
    sort: "createdAt,desc"
  });

  return response.content;
}

export async function createBackendWorkflowDefinition(
  input: CreateBackendWorkflowInput
) {
  return apiClient.post<CreateBackendWorkflowInput, BackendWorkflow>(
    endpoints.workflows.create,
    input
  );
}

export async function updateBackendWorkflowDefinition(
  workflowId: string,
  input: CreateBackendWorkflowInput
) {
  return apiClient.put<CreateBackendWorkflowInput, BackendWorkflow>(
    endpoints.workflows.byId(workflowId),
    input
  );
}

export async function updateBackendWorkflowStatus(
  workflowId: string,
  status: BackendWorkflowStatus
) {
  return apiClient.patch<{ status: BackendWorkflowStatus }, BackendWorkflow>(
    endpoints.workflows.status(workflowId),
    { status }
  );
}

export async function getBackendActivities() {
  const response = await apiClient.get<PageResponse<BackendActivity>>(
    `${endpoints.activities.list}?size=100&sort=createdAt,desc`
  );

  return response.content.map(toCropCycle);
}

export async function getBackendActivity(activityId: string) {
  const activity = await apiClient.get<BackendActivity>(
    endpoints.activities.byId(activityId)
  );

  return toCropCycle(activity);
}

export async function startBackendActivity(input: StartBackendActivityInput) {
  const activity = await apiClient.post<StartBackendActivityInput, BackendActivity>(
    endpoints.activities.start,
    input
  );

  return toCropCycle(activity);
}

export async function getBackendProofs(
  activities: CropCycle[] = [],
  domain?: WorkflowDomain
) {
  const evidence = await apiClient.get<BackendEvidence[]>(endpoints.evidence.list);
  const activityById = new Map(activities.map((activity) => [activity.id, activity]));
  const filteredEvidence = domain
    ? evidence.filter(
        (item) => normalizeWorkflowDomain(item.workflowDomainKey) === domain
      )
    : evidence;

  return filteredEvidence.map((item) =>
    toProofSubmission(item, activityById.get(item.activityId))
  );
}

export async function uploadBackendProof({
  activityId,
  activityTaskId,
  note,
  photoUri
}: UploadBackendEvidenceInput) {
  const formData = new FormData();
  formData.append("activityId", activityId);
  formData.append("activityTaskId", activityTaskId);
  if (note?.trim()) {
    formData.append("note", note.trim());
  }

  await appendUploadFile(
    formData,
    "file",
    { uri: photoUri },
    `evidence-${Date.now()}.jpg`
  );

  const evidence = await apiClient.postFormData<BackendEvidence>(
    endpoints.evidence.upload,
    formData
  );

  return {
    ...toProofSubmission(evidence),
    photoUri
  };
}

export async function reviewBackendProof({
  evidenceId,
  status
}: ReviewBackendEvidenceInput) {
  const evidence = await apiClient.patch<
    { status: ReviewBackendEvidenceInput["status"] },
    BackendEvidence
  >(endpoints.evidence.review(evidenceId), { status });

  return toProofSubmission(evidence);
}

export function upsertProofSubmission(
  proofs: ProofSubmission[],
  submission: ProofSubmission
) {
  return [
    submission,
    ...proofs.filter(
      (proof) =>
        (proof.activityId ?? proof.cycleId) !==
          (submission.activityId ?? submission.cycleId) ||
        (proof.taskId ?? proof.stepId) !== (submission.taskId ?? submission.stepId)
    )
  ];
}

function toCropTemplate(workflow: BackendWorkflow): CropTemplate {
  return {
    crop: workflow.name,
    durationDays: workflow.durationDays,
    id: workflow.id,
    name: workflow.name,
    tasks: workflow.tasks.map((task) => ({
      id: task.id,
      title: task.title,
      week: toWeek(task.offsetDays)
    })),
    tenantId: workflow.tenantId,
    steps: workflow.tasks
      .slice()
      .sort((left, right) => left.sequenceNumber - right.sequenceNumber)
      .map((task) => ({
        id: task.id,
        title: task.title,
        week: toWeek(task.offsetDays)
      }))
  };
}

function toCropCycle(activity: BackendActivity): CropCycle {
  const startedOn = toDisplayDate(activity.startedOn);
  const expectedCompletion = toDisplayDate(activity.expectedCompletion);
  const locationName = activity.locationName ?? "Unassigned location";
  const steps = activity.tasks
    .slice()
    .sort((left, right) => left.sequenceNumber - right.sequenceNumber)
    .map(toActivityTask);

  return {
    crop: activity.workflowName,
    expectedCompletion,
    expectedHarvest: expectedCompletion,
    farmerUsername: undefined,
    id: activity.id,
    locationName,
    participantName: activity.participantName ?? undefined,
    participantUsername: undefined,
    plot: activity.unitName,
    progress: activity.progressPercent,
    region: locationName,
    startedOn,
    status: toActivityStatus(activity.status),
    steps,
    tasks: steps,
    tenantId: activity.tenantId,
    unitName: activity.unitName,
    workflowDomainKey: activity.workflowDomainKey ?? undefined,
    workflowName: activity.workflowName
  };
}

function toActivityTask(task: BackendActivityTask): ActivityTask {
  return {
    due: toDisplayDate(task.dueOn),
    id: task.id,
    status: toTaskStatus(task.status),
    title: task.title
  };
}

function toProofSubmission(
  evidence: BackendEvidence,
  activity?: CropCycle
): ProofSubmission {
  const submittedAt = Date.parse(evidence.submittedAt);
  const participantName = evidence.participantName ?? "Participant";
  const crop =
    activity?.crop ?? activity?.workflowName ?? evidence.workflowName ?? "Workflow evidence";
  const region =
    activity?.region ??
    activity?.locationName ??
    evidence.locationName ??
    "Unassigned location";

  return {
    action: evidence.taskTitle,
    activityId: evidence.activityId,
    contentType: evidence.contentType,
    crop,
    cycleId: evidence.activityId,
    farmer: participantName,
    farmerUsername: undefined,
    id: evidence.id,
    locationName: region,
    note: evidence.note ?? undefined,
    participantName,
    participantUsername: undefined,
    region,
    sizeBytes: evidence.sizeBytes,
    status: toEvidenceStatus(evidence.status),
    stepId: evidence.activityTaskId,
    submittedAt: Number.isNaN(submittedAt) ? undefined : submittedAt,
    submittedOn: toDisplayDate(evidence.submittedAt),
    taskId: evidence.activityTaskId,
    taskTitle: evidence.taskTitle,
    unitName: activity?.unitName ?? evidence.unitName,
    workflowDomainKey: evidence.workflowDomainKey ?? undefined,
    workflowName: crop
  };
}

function toActivityStatus(status: BackendActivityStatus): ActivityStatus {
  if (status === "COMPLETED") {
    return "completed";
  }

  if (status === "CANCELLED") {
    return "cancelled";
  }

  return "running";
}

function toTaskStatus(status: BackendTaskStatus): TaskStatus {
  switch (status) {
    case "DONE":
    case "SKIPPED":
      return "done";
    case "NEXT":
      return "next";
    default:
      return "pending";
  }
}

function toEvidenceStatus(status: BackendEvidenceStatus): EvidenceStatus {
  if (status === "REJECTED") {
    return "rejected";
  }

  if (status === "APPROVED") {
    return "approved";
  }

  return "pending";
}

function toWeek(offsetDays: number) {
  return Math.max(1, Math.floor(offsetDays / 7) + 1);
}

function toDisplayDate(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  });
}

function normalizeWorkflowDomain(domainKey: string | null) {
  if (!domainKey) {
    return undefined;
  }

  return domainKey.toUpperCase() as WorkflowDomain;
}
