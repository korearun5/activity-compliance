export const userRoles = ["admin", "supervisor", "participant"] as const;

export type UserRole = (typeof userRoles)[number];

export type Account = {
  password: string;
  role: UserRole;
  tenantId?: string;
  username: string;
};

export type UserStatus = "Active" | "Inactive" | "Profile pending";

export type UserProfileInput = {
  age?: string;
  displayName: string;
  locationName: string;
  phone: string;
  sex?: string;
  siteName: string;
  status?: UserStatus;
};

export type UserProfileField = {
  label: string;
  value: string;
};

export type ManagedUser = {
  displayName: string;
  id?: string;
  locationName: string;
  phone: string;
  siteName: string;
  status: UserStatus;
  tenantId?: string;
  username: string;
};

export type TaskStatus = "done" | "next" | "pending";

export type ActivityStatus = "running" | "completed" | "cancelled";

export type EvidenceStatus = "approved" | "pending" | "rejected" | "done";

export type WorkflowTaskDefinition = {
  id: string;
  title: string;
  week: number;
};

export type WorkflowDefinition = {
  durationDays: number;
  id: string;
  name: string;
  tenantId?: string;
  tasks: WorkflowTaskDefinition[];
};

export type ActivityTask = {
  due: string;
  id: string;
  proof?: string;
  status: TaskStatus;
  title: string;
};

export type Activity = {
  expectedCompletion: string;
  id: string;
  locationName: string;
  participantName?: string;
  participantUsername?: string;
  progress: number;
  startedOn: string;
  status: ActivityStatus;
  tasks: ActivityTask[];
  tenantId?: string;
  unitName: string;
  workflowName: string;
};

export type Evidence = {
  activityId?: string;
  contentType?: string;
  id: string;
  locationName: string;
  note?: string;
  originalFilename?: string;
  participantName: string;
  participantUsername?: string;
  photoUri?: string;
  sizeBytes?: number;
  status: EvidenceStatus;
  storageKey?: string;
  submittedAt?: number;
  submittedOn: string;
  taskId?: string;
  tenantId?: string;
  taskTitle: string;
  workflowName: string;
};
