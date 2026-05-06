import {
  Activity,
  ActivityTask,
  Evidence,
  UserProfileField,
  WorkflowDefinition
} from "../core/model/types";

export type ProfileField = UserProfileField;

export type CropStep = ActivityTask;

export type CropCycle = Partial<Activity> & {
  crop: string;
  expectedHarvest: string;
  farmerUsername?: string;
  id: string;
  plot: string;
  progress: number;
  region: string;
  startedOn: string;
  status: NonNullable<Activity["status"]>;
  steps: CropStep[];
};

export type CropTemplate = Partial<Omit<WorkflowDefinition, "durationDays">> & {
  crop: string;
  durationDays: number;
  steps: Array<{
    id: string;
    title: string;
    week: number;
  }>;
};

export type ProofSubmission = Partial<Evidence> & {
  action: string;
  cycleId?: string;
  crop: string;
  farmer: string;
  farmerUsername?: string;
  id: string;
  note?: string;
  photoUri?: string;
  region: string;
  status: NonNullable<Evidence["status"]>;
  stepId?: string;
  submittedAt?: number;
  submittedOn: string;
};

export const profileTemplate: ProfileField[] = [
  { label: "Name", value: "Not set" },
  { label: "Age", value: "Not set" },
  { label: "Sex", value: "Not set" },
  { label: "Phone", value: "Not set" },
  { label: "Region", value: "Not set" },
  { label: "Village", value: "Not set" },
  { label: "Farm size", value: "Not set" },
  { label: "Soil type", value: "Not set" },
  { label: "Crop type", value: "Not set" },
  { label: "Crop start date", value: "Not set" },
  { label: "Expected harvest date", value: "Not set" },
  { label: "Irrigation type", value: "Not set" },
  { label: "Farmer group", value: "Not set" },
  { label: "Status", value: "Active" }
];

export const cropCycles: CropCycle[] = [];

export const cropTemplates: CropTemplate[] = [];

export const proofSubmissions: ProofSubmission[] = [];
