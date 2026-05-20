import { Id } from "../../../core/api/contracts";

export type CarbonParticipantType = "AGRONOMIST" | "FARMER" | "FPO_FPC";
export type CarbonRecordStatus = "ACTIVE" | "ARCHIVED" | "INACTIVE" | "SUSPENDED";
export type CarbonActivityVerificationStatus =
  | "PENDING_EVIDENCE"
  | "PENDING_REVIEW"
  | "REJECTED"
  | "VERIFIED";

export type CarbonActivityCategoryResponse = {
  code: string;
  createdAt: string;
  description: string;
  evidenceRequired: boolean;
  id: Id;
  name: string;
  sortOrder: number;
  status: CarbonRecordStatus;
  updatedAt: string;
};

export type CarbonProfileResponse = {
  aadhaarStatus: string | null;
  bankStatus: string | null;
  carbonIdentityId: string;
  coordinatorUserId: Id | null;
  createdAt: string;
  croppingPattern: string | null;
  displayName: string;
  districtName: string | null;
  documentStatus: string | null;
  fpoMemberProfileId: Id | null;
  aadhaarNumber: string | null;
  age: number | null;
  alternateMobileNumber: string | null;
  farmerCategory: string | null;
  gender: string | null;
  gpsLatitude: number | null;
  gpsLongitude: number | null;
  id: Id;
  livestockCount: number | null;
  memberNumber: string | null;
  mobileNumber: string | null;
  participantType: CarbonParticipantType;
  stateName: string | null;
  status: CarbonRecordStatus;
  taluka: string | null;
  tenantId: Id;
  tillageStatus: string | null;
  totalLandHoldingAcres: number | null;
  updatedAt: string;
  userId: Id | null;
  username: string | null;
  village: string | null;
};

export type CarbonProfileRequest = {
  aadhaarNumber?: string;
  aadhaarStatus?: string;
  age?: number;
  alternateMobileNumber?: string;
  bankStatus?: string;
  carbonIdentityId?: string;
  coordinatorUserId?: Id;
  croppingPattern?: string;
  displayName?: string;
  districtName?: string;
  documentStatus?: string;
  farmerCategory?: string;
  fpoMemberProfileId?: Id;
  gender?: string;
  gpsLatitude?: number;
  gpsLongitude?: number;
  livestockCount?: number;
  memberNumber?: string;
  mobileNumber?: string;
  participantType?: CarbonParticipantType;
  password?: string;
  stateName?: string;
  status?: CarbonRecordStatus;
  taluka?: string;
  tillageStatus?: string;
  totalLandHoldingAcres?: number;
  userId?: Id;
  username?: string;
  village?: string;
};

export type CarbonFarmPlotResponse = {
  areaAcres: number;
  blockCode: string | null;
  carbonProfileId: Id;
  createdAt: string;
  farmName: string;
  id: Id;
  irrigationSource: string | null;
  latitude: number;
  longitude: number;
  plantingDate: string | null;
  primaryCrop: string | null;
  rootstock: string | null;
  rowCount: number | null;
  spacing: string | null;
  status: CarbonRecordStatus;
  surveyNumber: string | null;
  tenantId: Id;
  tillageStatus: string | null;
  updatedAt: string;
  variety: string | null;
};

export type CarbonFarmPlotRequest = {
  areaAcres: number;
  blockCode?: string;
  farmName: string;
  irrigationSource?: string;
  latitude: number;
  longitude: number;
  plantingDate?: string;
  primaryCrop?: string;
  rootstock?: string;
  rowCount?: number;
  spacing?: string;
  status?: CarbonRecordStatus;
  surveyNumber?: string;
  tillageStatus?: string;
  variety?: string;
};

export type CarbonSoilProfileResponse = {
  bulkDensityGmCm3: number | null;
  carbonFarmPlotId: Id | null;
  carbonProfileId: Id;
  createdAt: string;
  electricalConductivity: number | null;
  id: Id;
  labName: string | null;
  nitrogenKgHa: number | null;
  ph: number | null;
  phosphorusKgHa: number | null;
  potassiumKgHa: number | null;
  reportContentType: string | null;
  reportFileName: string | null;
  reportStorageKey: string | null;
  reportUrl: string | null;
  soilOrganicCarbonPercent: number | null;
  status: CarbonRecordStatus;
  tenantId: Id;
  testDate: string | null;
  texture: string | null;
  updatedAt: string;
};

export type CarbonSoilProfileRequest = {
  bulkDensityGmCm3?: number;
  carbonFarmPlotId?: Id;
  electricalConductivity?: number;
  labName?: string;
  nitrogenKgHa?: number;
  ph?: number;
  phosphorusKgHa?: number;
  potassiumKgHa?: number;
  reportContentType?: string;
  reportFileName?: string;
  reportStorageKey?: string;
  reportUrl?: string;
  soilOrganicCarbonPercent?: number;
  status?: CarbonRecordStatus;
  testDate?: string;
  texture?: string;
};

export type CarbonActivityRecordResponse = {
  activityDate: string;
  carbonFarmPlotId: Id | null;
  carbonProfileId: Id;
  categoryCode: string;
  categoryId: Id;
  categoryName: string;
  createdAt: string;
  cropName: string;
  evidenceCount: number;
  evidenceRequired: boolean;
  farmName: string | null;
  id: Id;
  inputUsed: string | null;
  quantityUnit: string | null;
  quantityValue: number | null;
  remarks: string | null;
  status: CarbonRecordStatus;
  tenantId: Id;
  updatedAt: string;
  verificationStatus: CarbonActivityVerificationStatus;
};

export type CarbonActivityRecordRequest = {
  activityDate: string;
  carbonFarmPlotId?: Id;
  categoryId: Id;
  cropName: string;
  inputUsed?: string;
  quantityUnit?: string;
  quantityValue?: number;
  remarks?: string;
  status?: CarbonRecordStatus;
};
