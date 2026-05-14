import { Id } from "./contracts";

export type FpoMemberStatus = "ACTIVE" | "INACTIVE" | "SUSPENDED";
export type FarmRecordStatus = "ACTIVE" | "ARCHIVED" | "INACTIVE";
export type AdvisoryStatus = "ARCHIVED" | "DRAFT" | "PUBLISHED";
export type AdvisoryTargetType = "ALL_MEMBERS" | "MEMBER" | "VILLAGE";
export type NotificationChannel = "EMAIL" | "IN_APP" | "PUSH" | "SMS";

export type FpoMemberResponse = {
  age: number | null;
  aadhaarNumber: string | null;
  alternateMobileNumber: string | null;
  coordinatorName: string | null;
  coordinatorUserId: Id | null;
  createdAt: string;
  dateOfBirth: string | null;
  displayName: string;
  districtName: string | null;
  farmerCategory: string | null;
  gender: string | null;
  id: Id;
  memberNumber: string;
  mobileNumber: string;
  status: FpoMemberStatus;
  stateName: string;
  taluka: string;
  tenantId: Id;
  updatedAt: string;
  userId: Id;
  username: string;
  village: string;
};

export type CreateFpoMemberRequest = {
  age?: number;
  aadhaarNumber?: string;
  alternateMobileNumber?: string;
  coordinatorUserId?: Id;
  dateOfBirth?: string;
  displayName: string;
  districtName?: string;
  farmerCategory?: string;
  gender?: string;
  memberNumber: string;
  mobileNumber: string;
  password?: string;
  status?: FpoMemberStatus;
  stateName: string;
  taluka: string;
  userId?: Id;
  username?: string;
  village: string;
};

export type UpdateFpoMemberRequest = Omit<
  CreateFpoMemberRequest,
  "password" | "userId" | "username"
> & {
  status: FpoMemberStatus;
};

export type FarmLandholdingResponse = {
  createdAt: string;
  cultivableAreaAcres: number | null;
  id: Id;
  irrigationSource: string;
  memberId: Id;
  memberNumber: string;
  ownershipType: string;
  status: FarmRecordStatus;
  surveyNumber: string;
  tenantId: Id;
  totalAreaAcres: number;
  updatedAt: string;
};

export type CreateFarmLandholdingRequest = {
  cultivableAreaAcres?: number;
  irrigationSource: string;
  ownershipType: string;
  status?: FarmRecordStatus;
  surveyNumber: string;
  totalAreaAcres: number;
};

export type UpdateFarmLandholdingRequest = Required<
  Pick<CreateFarmLandholdingRequest, "status" | "totalAreaAcres">
> &
  Omit<CreateFarmLandholdingRequest, "status" | "totalAreaAcres">;

export type FarmPlotResponse = {
  areaAcres: number;
  createdAt: string;
  id: Id;
  landholdingId: Id | null;
  latitude: number;
  longitude: number;
  memberId: Id;
  memberNumber: string;
  plotName: string;
  soilType: string | null;
  status: FarmRecordStatus;
  tenantId: Id;
  updatedAt: string;
};

export type CreateFarmPlotRequest = {
  areaAcres: number;
  landholdingId?: Id;
  latitude: number;
  longitude: number;
  plotName: string;
  soilType?: string;
  status?: FarmRecordStatus;
};

export type UpdateFarmPlotRequest = Required<
  Pick<CreateFarmPlotRequest, "areaAcres" | "plotName" | "status">
> &
  Omit<CreateFarmPlotRequest, "areaAcres" | "plotName" | "status">;

export type UpdateFarmRecordStatusRequest = {
  status: FarmRecordStatus;
};

export type FpoSoilProfileResponse = {
  createdAt: string;
  id: Id;
  memberId: Id;
  memberNumber: string;
  nitrogen: number | null;
  notes: string | null;
  ph: number | null;
  phosphorus: number | null;
  potassium: number | null;
  reportContentType: string | null;
  reportFileName: string | null;
  reportUrl: string | null;
  soilOrganicCarbon: number | null;
  tenantId: Id;
  updatedAt: string;
};

export type FpoSoilProfileRequest = {
  nitrogen?: number;
  notes?: string;
  ph?: number;
  phosphorus?: number;
  potassium?: number;
  reportContentType?: string;
  reportFileName?: string;
  reportUrl?: string;
  soilOrganicCarbon?: number;
};

export type CropPlanStatus = "CANCELLED" | "COMPLETED" | "CONFIRMED" | "DRAFT";

export type CropCatalogResponse = {
  category: string | null;
  code: string;
  createdAt: string;
  id: Id;
  name: string;
  status: FarmRecordStatus;
  tenantId: Id;
  updatedAt: string;
};

export type CropCatalogRequest = {
  category?: string;
  code: string;
  name: string;
  status?: FarmRecordStatus;
};

export type CropSeasonResponse = {
  code: string;
  createdAt: string;
  endMonth: number | null;
  id: Id;
  name: string;
  seasonYear: number;
  startMonth: number | null;
  status: FarmRecordStatus;
  tenantId: Id;
  updatedAt: string;
};

export type CropSeasonRequest = {
  code: string;
  endMonth?: number;
  name: string;
  seasonYear: number;
  startMonth?: number;
  status?: FarmRecordStatus;
};

export type CropHistoryResponse = {
  areaAcres: number | null;
  createdAt: string;
  cropCode: string;
  cropId: Id;
  cropName: string;
  cropYear: number;
  id: Id;
  memberId: Id;
  memberName: string;
  memberNumber: string;
  notes: string | null;
  seasonCode: string | null;
  seasonId: Id | null;
  seasonName: string | null;
  tenantId: Id;
  updatedAt: string;
  yieldQuantity: number | null;
  yieldUnit: string | null;
};

export type CropHistoryRequest = {
  areaAcres?: number;
  cropId: Id;
  cropYear: number;
  notes?: string;
  seasonId?: Id;
  yieldQuantity?: number;
  yieldUnit?: string;
};

export type CropPlanResponse = {
  confirmedAt: string | null;
  createdAt: string;
  cropCode: string;
  cropId: Id;
  cropName: string;
  expectedHarvestDate: string | null;
  expectedYieldQuintals: number | null;
  id: Id;
  memberId: Id;
  memberName: string;
  memberNumber: string;
  memberVillage: string;
  plannedAreaAcres: number;
  plannedSowingDate: string | null;
  plotId: Id | null;
  plotName: string | null;
  seasonCode: string;
  seasonId: Id;
  seasonName: string;
  seasonYear: number;
  cropYear: string;
  status: CropPlanStatus;
  tenantId: Id;
  updatedAt: string;
};

export type CropPlanRequest = {
  cropId: Id;
  cropYear: string;
  expectedHarvestDate?: string;
  expectedYieldQuintals?: number;
  memberId: Id;
  plannedAreaAcres: number;
  plannedSowingDate?: string;
  plotId?: Id;
  seasonId: Id;
  status?: CropPlanStatus;
};

export type UpdateCropPlanStatusRequest = {
  status: CropPlanStatus;
};

export type InputDemandEstimateStatus = "ESTIMATED" | "SUPERSEDED";

export type InputCatalogResponse = {
  category: string | null;
  code: string;
  createdAt: string;
  id: Id;
  name: string;
  status: FarmRecordStatus;
  tenantId: Id;
  unit: string;
  updatedAt: string;
};

export type InputCatalogRequest = {
  category?: string;
  code: string;
  name: string;
  status?: FarmRecordStatus;
  unit: string;
};

export type CropInputRuleResponse = {
  applicationStage: string | null;
  createdAt: string;
  cropCode: string;
  cropId: Id;
  cropName: string;
  id: Id;
  inputCode: string;
  inputId: Id;
  inputName: string;
  inputUnit: string;
  notes: string | null;
  quantityPerAcre: number;
  status: FarmRecordStatus;
  tenantId: Id;
  updatedAt: string;
};

export type CropInputRuleRequest = {
  applicationStage?: string;
  cropId: Id;
  inputId: Id;
  notes?: string;
  quantityPerAcre: number;
  status?: FarmRecordStatus;
};

export type InputDemandRunRequest = {
  cropId?: Id;
  planStatus?: CropPlanStatus;
  seasonId: Id;
  village?: string;
};

export type InputDemandEstimateResponse = {
  createdAt: string;
  cropCode: string;
  cropId: Id;
  cropName: string;
  cropPlanId: Id;
  estimatedQuantity: number;
  id: Id;
  inputCategory: string | null;
  inputCode: string;
  inputId: Id;
  inputName: string;
  memberId: Id;
  memberName: string;
  memberNumber: string;
  memberVillage: string;
  bufferPercent: number;
  bufferQuantity: number;
  finalDemandQuantity: number;
  recommendedQuantityPerAcre: number;
  seasonCode: string;
  seasonId: Id;
  seasonName: string;
  seasonYear: number;
  status: InputDemandEstimateStatus;
  totalDemandQuantity: number;
  tenantId: Id;
  unit: string;
  updatedAt: string;
};

export type InputDemandRunResponse = {
  cropId: Id | null;
  estimates: InputDemandEstimateResponse[];
  estimatesGenerated: number;
  missingRulePlanCount: number;
  plansConsidered: number;
  planStatus: string;
  seasonId: Id;
  village: string | null;
};

export type InputDemandByInputResponse = {
  estimatedQuantity: number;
  inputCode: string;
  inputId: Id;
  inputName: string;
  bufferQuantity: number;
  finalDemandQuantity: number;
  planCount: number;
  totalDemandQuantity: number;
  unit: string;
};

export type InputDemandByCropResponse = {
  cropId: Id;
  cropName: string;
  plannedAreaAcres: number;
  planCount: number;
};

export type InputDemandByVillageResponse = {
  memberCount: number;
  planCount: number;
  plannedAreaAcres: number;
  village: string;
};

export type InputDemandSummaryResponse = {
  byCrop: InputDemandByCropResponse[];
  byInput: InputDemandByInputResponse[];
  byVillage: InputDemandByVillageResponse[];
  cropId: Id | null;
  estimateCount: number;
  memberCount: number;
  planCount: number;
  seasonId: Id | null;
  totalPlannedAreaAcres: number;
  village: string | null;
};

export type FpoAreaBreakdownResponse = {
  areaAcres: number;
  id: Id | null;
  label: string;
  memberCount: number;
  planCount: number;
};

export type FpoInputDemandBreakdownResponse = {
  estimatedQuantity: number;
  inputCode: string;
  inputId: Id;
  inputName: string;
  memberCount: number;
  planCount: number;
  unit: string;
};

export type FpoDashboardSummaryResponse = {
  activeLandAreaAcres: number;
  activeLandholdings: number;
  activeMembers: number;
  activePlotAreaAcres: number;
  activePlots: number;
  confirmedCropPlanCount: number;
  confirmedPlannedAreaAcres: number;
  cropPlanAreaByCrop: FpoAreaBreakdownResponse[];
  cropPlanAreaBySeason: FpoAreaBreakdownResponse[];
  cropPlanAreaByVillage: FpoAreaBreakdownResponse[];
  cropPlanCount: number;
  demandEstimateCount: number;
  geoTaggedPlots: number;
  inputDemandByInput: FpoInputDemandBreakdownResponse[];
  tenantId: Id;
  totalCultivableAreaAcres: number;
  totalLandAreaAcres: number;
  totalLandholdings: number;
  totalMembers: number;
  totalPlotAreaAcres: number;
  totalPlots: number;
};

export type FpoAdvisoryResponse = {
  channel: NotificationChannel;
  createdAt: string;
  createdByName: string | null;
  createdByUserId: Id | null;
  cropId: Id | null;
  cropName: string | null;
  id: Id;
  message: string;
  publishedAt: string | null;
  seasonId: Id | null;
  seasonName: string | null;
  seasonYear: number | null;
  status: AdvisoryStatus;
  targetMemberId: Id | null;
  targetMemberName: string | null;
  targetType: AdvisoryTargetType;
  targetVillage: string | null;
  tenantId: Id;
  title: string;
  updatedAt: string;
};

export type FpoAdvisoryRequest = {
  channel?: NotificationChannel;
  cropId?: Id;
  message: string;
  seasonId?: Id;
  status?: AdvisoryStatus;
  targetMemberId?: Id;
  targetType?: AdvisoryTargetType;
  targetVillage?: string;
  title: string;
};

export type UpdateFpoAdvisoryStatusRequest = {
  status: AdvisoryStatus;
};
