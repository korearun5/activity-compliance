import {
  Activity,
  ActivityTask,
  Evidence,
  UserProfileField,
  WorkflowDefinition
} from "../core/model/types";

export type FarmerProfileField = UserProfileField;

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

export const farmerProfile: FarmerProfileField[] = [
  { label: "Name", value: "Ravi Kumar" },
  { label: "Age", value: "42" },
  { label: "Sex", value: "Male" },
  { label: "Phone", value: "+91 98765 43210" },
  { label: "Region", value: "North Block" },
  { label: "Village", value: "Rampur" },
  { label: "Farm size", value: "2.5 acres" },
  { label: "Soil type", value: "Loamy" },
  { label: "Crop type", value: "Vegetable" },
  { label: "Crop start date", value: "01 May 2026" },
  { label: "Expected harvest date", value: "30 Jul 2026" },
  { label: "Irrigation type", value: "Drip" },
  { label: "Farmer group", value: "Village Group A" },
  { label: "Status", value: "Active" }
];

export const cropCycles: CropCycle[] = [
  {
    id: "tomato-2026-05",
    crop: "Tomato",
    region: "North Block",
    plot: "Plot A",
    startedOn: "01 May 2026",
    expectedHarvest: "30 Jul 2026",
    progress: 38,
    status: "running",
    steps: [
      {
        id: "land-prep",
        title: "Land preparation",
        due: "Week 1",
        status: "done",
        proof: "Photo approved"
      },
      {
        id: "sowing",
        title: "Seed sowing",
        due: "Week 2",
        status: "done",
        proof: "Photo approved"
      },
      {
        id: "irrigation",
        title: "First irrigation check",
        due: "Week 3",
        status: "next"
      },
      {
        id: "fertilizer",
        title: "Fertilizer application",
        due: "Week 4",
        status: "pending"
      },
      {
        id: "pest-check",
        title: "Pest control review",
        due: "Week 6",
        status: "pending"
      }
    ]
  },
  {
    id: "chilli-2026-04",
    crop: "Chilli",
    region: "North Block",
    plot: "Plot B",
    startedOn: "10 Apr 2026",
    expectedHarvest: "15 Aug 2026",
    progress: 55,
    status: "running",
    steps: [
      {
        id: "nursery",
        title: "Nursery preparation",
        due: "Week 1",
        status: "done",
        proof: "Photo approved"
      },
      {
        id: "transplant",
        title: "Transplanting",
        due: "Week 4",
        status: "next"
      },
      {
        id: "mulching",
        title: "Mulching",
        due: "Week 5",
        status: "pending"
      }
    ]
  },
  {
    id: "okra-2026-01",
    crop: "Okra",
    region: "North Block",
    plot: "Plot C",
    startedOn: "05 Jan 2026",
    expectedHarvest: "05 Apr 2026",
    progress: 100,
    status: "completed",
    steps: [
      {
        id: "harvest",
        title: "Harvest completed",
        due: "Week 12",
        status: "done",
        proof: "Final proof approved"
      }
    ]
  }
];

export const cropTemplates: CropTemplate[] = [
  {
    crop: "Tomato",
    durationDays: 90,
    steps: [
      { id: "land-prep", title: "Land preparation", week: 1 },
      { id: "sowing", title: "Seed sowing", week: 2 },
      { id: "irrigation", title: "First irrigation check", week: 3 },
      { id: "fertilizer", title: "Fertilizer application", week: 4 },
      { id: "pest-check", title: "Pest control review", week: 6 },
      { id: "harvest", title: "Harvest", week: 12 }
    ]
  },
  {
    crop: "Chilli",
    durationDays: 120,
    steps: [
      { id: "nursery", title: "Nursery preparation", week: 1 },
      { id: "transplant", title: "Transplanting", week: 4 },
      { id: "mulching", title: "Mulching", week: 5 },
      { id: "fertilizer", title: "Fertilizer application", week: 7 },
      { id: "pest-check", title: "Pest control review", week: 9 },
      { id: "harvest", title: "Harvest", week: 16 }
    ]
  },
  {
    crop: "Okra",
    durationDays: 75,
    steps: [
      { id: "land-prep", title: "Land preparation", week: 1 },
      { id: "sowing", title: "Seed sowing", week: 1 },
      { id: "irrigation", title: "Irrigation check", week: 2 },
      { id: "weed-removal", title: "Weed removal", week: 4 },
      { id: "harvest", title: "Harvest", week: 10 }
    ]
  }
];

export const proofSubmissions: ProofSubmission[] = [
  {
    id: "proof-1",
    farmer: "Ravi Kumar",
    crop: "Tomato",
    region: "North Block",
    action: "Land preparation",
    submittedOn: "02 May 2026",
    status: "approved"
  },
  {
    id: "proof-2",
    farmer: "Meena Devi",
    crop: "Chilli",
    region: "East Block",
    action: "Transplanting",
    submittedOn: "03 May 2026",
    status: "pending"
  },
  {
    id: "proof-3",
    farmer: "Iqbal Khan",
    crop: "Tomato",
    region: "North Block",
    action: "Irrigation check",
    submittedOn: "04 May 2026",
    status: "pending"
  }
];
