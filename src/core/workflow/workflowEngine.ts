import { appConfig } from "../config/appConfig";
import {
  ActivityTask,
  Evidence,
  EvidenceStatus,
  TaskStatus
} from "../model/types";

type TaskLike = {
  id: string;
  status: TaskStatus;
};

type ActivityLike<TTask extends TaskLike> = {
  id: string;
  steps?: TTask[];
  tasks?: TTask[];
};

type EvidenceLike = {
  activityId?: string;
  cycleId?: string;
  status: EvidenceStatus;
  stepId?: string;
  submittedAt?: number;
  taskId?: string;
};

export function activateNextPendingTask<TTask extends TaskLike>(
  tasks: TTask[]
) {
  if (tasks.some((task) => task.status === "next")) {
    return tasks;
  }

  let activated = false;

  return tasks.map((task) => {
    if (!activated && task.status === "pending") {
      activated = true;
      return { ...task, status: "next" as const };
    }

    return task;
  });
}

export function calculateTaskProgress(tasks: TaskLike[]) {
  if (!tasks.length) {
    return 0;
  }

  const doneCount = tasks.filter((task) => task.status === "done").length;
  return Math.round((doneCount / tasks.length) * 100);
}

export function findEvidenceForTask<TEvidence extends EvidenceLike>(
  evidenceRecords: TEvidence[],
  activityId: string,
  taskId: string
) {
  return evidenceRecords.find(
    (evidence) =>
      (evidence.activityId ?? evidence.cycleId) === activityId &&
      (evidence.taskId ?? evidence.stepId) === taskId &&
      evidence.status === "done"
  );
}

export function isActivityComplete<
  TActivity extends ActivityLike<TTask>,
  TTask extends TaskLike,
  TEvidence extends EvidenceLike
>(activity: TActivity, evidenceRecords: TEvidence[]) {
  const tasks = activity.tasks ?? activity.steps ?? [];

  return tasks.every(
    (task) =>
      task.status === "done" ||
      Boolean(findEvidenceForTask(evidenceRecords, activity.id, task.id))
  );
}

export function isEvidenceEditable(evidence: Pick<Evidence, "submittedAt">) {
  const submittedAt = evidence.submittedAt ?? 0;
  return Date.now() - submittedAt <= appConfig.proofEditWindowMs;
}

export function toActivityTaskStatus(status: string): TaskStatus {
  return status === "done" || status === "next" ? status : "pending";
}

export type { ActivityTask };

