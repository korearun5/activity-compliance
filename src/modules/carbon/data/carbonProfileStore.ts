import { apiClient, ApiClientError } from "../../../core/api/client";
import { PageResponse } from "../../../core/api/contracts";
import { endpoints } from "../../../core/api/endpoints";
import { appendUploadFile, UploadFileInput } from "../../../core/api/uploadFile";
import { AppError } from "../../../core/errors/AppError";
import {
  CarbonActivityCategoryResponse,
  CarbonActivityRecordRequest,
  CarbonActivityRecordResponse,
  CarbonActivityVerificationStatus,
  CarbonFarmPlotRequest,
  CarbonFarmPlotResponse,
  CarbonParticipantType,
  CarbonProfileRequest,
  CarbonProfileResponse,
  CarbonRecordStatus,
  CarbonSoilProfileRequest,
  CarbonSoilProfileResponse
} from "../api/carbonContracts";

export const CARBON_PARTICIPANT_TYPES: CarbonParticipantType[] = [
  "FARMER",
  "FPO_FPC",
  "AGRONOMIST"
];

export const CARBON_RECORD_STATUSES: CarbonRecordStatus[] = [
  "ACTIVE",
  "INACTIVE",
  "SUSPENDED",
  "ARCHIVED"
];

export const CARBON_TILLAGE_STATUSES = [
  "Conventional",
  "Reduced tillage",
  "No tillage"
] as const;
export const CARBON_BANK_STATUSES = ["Linked", "Pending", "Not required"] as const;
export const CARBON_AADHAAR_STATUSES = ["Provided", "Optional not captured"] as const;
export const CARBON_DOCUMENT_STATUSES = ["Not started", "Partial", "Ready"] as const;
export type CarbonProfileRecord = {
  aadhaarNumber?: string;
  aadhaarStatus?: string;
  age?: number;
  alternateMobileNumber?: string;
  bankStatus?: string;
  carbonIdentityId: string;
  coordinatorUserId?: string;
  createdAt: string;
  croppingPattern?: string;
  displayName: string;
  districtName?: string;
  documentStatus?: string;
  farmerCategory?: string;
  fpoMemberProfileId?: string;
  gender?: string;
  gpsLatitude?: number;
  gpsLongitude?: number;
  id: string;
  livestockCount?: number;
  memberNumber?: string;
  mobileNumber?: string;
  participantType: CarbonParticipantType;
  stateName?: string;
  status: CarbonRecordStatus;
  taluka?: string;
  tenantId: string;
  tillageStatus?: string;
  totalLandHoldingAcres?: number;
  updatedAt: string;
  userId?: string;
  username?: string;
  village?: string;
};

export type CarbonProfileInput = {
  aadhaarNumber?: string;
  aadhaarStatus?: string;
  age?: string;
  alternateMobileNumber?: string;
  bankStatus?: string;
  carbonIdentityId?: string;
  coordinatorUserId?: string;
  croppingPattern?: string;
  displayName: string;
  districtName?: string;
  documentStatus?: string;
  farmerCategory?: string;
  fpoMemberProfileId?: string;
  gender?: string;
  gpsLatitude?: string;
  gpsLongitude?: string;
  livestockCount?: string;
  memberNumber?: string;
  mobileNumber?: string;
  participantType: CarbonParticipantType;
  password?: string;
  stateName?: string;
  status?: CarbonRecordStatus;
  taluka?: string;
  tillageStatus?: string;
  totalLandHoldingAcres?: string;
  userId?: string;
  username?: string;
  village?: string;
};

export type CarbonFarmPlotRecord = {
  areaAcres: number;
  carbonProfileId: string;
  createdAt: string;
  farmName: string;
  id: string;
  irrigationSource?: string;
  latitude: number;
  longitude: number;
  primaryCrop?: string;
  status: CarbonRecordStatus;
  surveyNumber?: string;
  tenantId: string;
  tillageStatus?: string;
  updatedAt: string;
};

export type CarbonFarmPlotInput = {
  areaAcres: string;
  farmName: string;
  irrigationSource?: string;
  latitude: string;
  longitude: string;
  primaryCrop?: string;
  status?: CarbonRecordStatus;
  surveyNumber?: string;
  tillageStatus?: string;
};

export type CarbonSoilProfileRecord = {
  bulkDensityGmCm3?: number;
  carbonFarmPlotId?: string;
  carbonProfileId: string;
  createdAt: string;
  electricalConductivity?: number;
  id: string;
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
  status: CarbonRecordStatus;
  tenantId: string;
  testDate?: string;
  texture?: string;
  updatedAt: string;
};

export type CarbonActivityCategoryRecord = {
  code: string;
  description: string;
  evidenceRequired: boolean;
  id: string;
  name: string;
  sortOrder: number;
  status: CarbonRecordStatus;
};

export type CarbonActivityRecord = {
  activityDate: string;
  carbonFarmPlotId?: string;
  carbonProfileId: string;
  categoryCode: string;
  categoryId: string;
  categoryName: string;
  cropName: string;
  evidenceCount: number;
  evidenceRequired: boolean;
  farmName?: string;
  id: string;
  inputUsed?: string;
  quantityUnit?: string;
  quantityValue?: number;
  remarks?: string;
  status: CarbonRecordStatus;
  tenantId: string;
  updatedAt: string;
  verificationStatus: CarbonActivityVerificationStatus;
};

export type CarbonSoilProfileInput = {
  bulkDensityGmCm3?: string;
  carbonFarmPlotId?: string;
  electricalConductivity?: string;
  labName?: string;
  nitrogenKgHa?: string;
  ph?: string;
  phosphorusKgHa?: string;
  potassiumKgHa?: string;
  reportContentType?: string;
  reportFileName?: string;
  reportStorageKey?: string;
  reportUrl?: string;
  soilOrganicCarbonPercent?: string;
  status?: CarbonRecordStatus;
  testDate?: string;
  texture?: string;
};

export type CarbonActivityInput = {
  activityDate: string;
  carbonFarmPlotId?: string;
  categoryId: string;
  cropName: string;
  inputUsed?: string;
  quantityUnit?: string;
  quantityValue?: string;
  remarks?: string;
  status?: CarbonRecordStatus;
};

export type CarbonSoilReportFile = UploadFileInput;

export async function listCarbonProfiles(): Promise<CarbonProfileRecord[]> {
  try {
    const page = await apiClient.getPaginated<PageResponse<CarbonProfileResponse>>(
      endpoints.carbon.profiles.list,
      { size: 100, sort: "createdAt,desc" }
    );

    return page.content.map(toCarbonProfile);
  } catch (error) {
    throw toCarbonError(error, "Unable to load Carbon profiles.");
  }
}

export async function listCarbonActivityCategories(): Promise<
  CarbonActivityCategoryRecord[]
> {
  try {
    const response = await apiClient.get<CarbonActivityCategoryResponse[]>(
      endpoints.carbon.activityCategories.list
    );

    return response.map(toCarbonActivityCategory);
  } catch (error) {
    throw toCarbonError(error, "Unable to load Carbon activity categories.");
  }
}

export async function getMyCarbonProfile(): Promise<CarbonProfileRecord> {
  try {
    return toCarbonProfile(
      await apiClient.get<CarbonProfileResponse>(endpoints.carbon.profiles.me)
    );
  } catch (error) {
    throw toCarbonError(error, "Unable to load your Carbon profile.");
  }
}

export async function createCarbonProfile(
  input: CarbonProfileInput
): Promise<CarbonProfileRecord> {
  try {
    return toCarbonProfile(
      await apiClient.post<CarbonProfileRequest, CarbonProfileResponse>(
        endpoints.carbon.profiles.create,
        toCarbonProfileRequest(input)
      )
    );
  } catch (error) {
    throw toCarbonError(error, "Unable to save Carbon profile.");
  }
}

export async function updateCarbonProfile(
  profileId: string,
  input: CarbonProfileInput
): Promise<CarbonProfileRecord> {
  try {
    return toCarbonProfile(
      await apiClient.put<CarbonProfileRequest, CarbonProfileResponse>(
        endpoints.carbon.profiles.byId(profileId),
        toCarbonProfileRequest(input)
      )
    );
  } catch (error) {
    throw toCarbonError(error, "Unable to update Carbon profile.");
  }
}

export async function listCarbonFarmPlots(
  profileId: string
): Promise<CarbonFarmPlotRecord[]> {
  try {
    const response = await apiClient.get<CarbonFarmPlotResponse[]>(
      endpoints.carbon.plots.listByProfile(profileId)
    );

    return response.map(toCarbonFarmPlot);
  } catch (error) {
    throw toCarbonError(error, "Unable to load Carbon farm plots.");
  }
}

export async function createCarbonFarmPlot(
  profileId: string,
  input: CarbonFarmPlotInput
): Promise<CarbonFarmPlotRecord> {
  try {
    return toCarbonFarmPlot(
      await apiClient.post<CarbonFarmPlotRequest, CarbonFarmPlotResponse>(
        endpoints.carbon.plots.createForProfile(profileId),
        toCarbonFarmPlotRequest(input)
      )
    );
  } catch (error) {
    throw toCarbonError(error, "Unable to save Carbon farm plot.");
  }
}

export async function updateCarbonFarmPlot(
  plotId: string,
  input: CarbonFarmPlotInput
): Promise<CarbonFarmPlotRecord> {
  try {
    return toCarbonFarmPlot(
      await apiClient.put<CarbonFarmPlotRequest, CarbonFarmPlotResponse>(
        endpoints.carbon.plots.byId(plotId),
        toCarbonFarmPlotRequest(input)
      )
    );
  } catch (error) {
    throw toCarbonError(error, "Unable to update Carbon farm plot.");
  }
}

export async function listCarbonSoilProfiles(
  profileId: string
): Promise<CarbonSoilProfileRecord[]> {
  try {
    const response = await apiClient.get<CarbonSoilProfileResponse[]>(
      endpoints.carbon.soilProfiles.listByProfile(profileId)
    );

    return response.map(toCarbonSoilProfile);
  } catch (error) {
    throw toCarbonError(error, "Unable to load Carbon soil profiles.");
  }
}

export async function createCarbonSoilProfile(
  profileId: string,
  input: CarbonSoilProfileInput
): Promise<CarbonSoilProfileRecord> {
  try {
    return toCarbonSoilProfile(
      await apiClient.post<CarbonSoilProfileRequest, CarbonSoilProfileResponse>(
        endpoints.carbon.soilProfiles.createForProfile(profileId),
        toCarbonSoilProfileRequest(input)
      )
    );
  } catch (error) {
    throw toCarbonError(error, "Unable to save Carbon soil profile.");
  }
}

export async function updateCarbonSoilProfile(
  soilProfileId: string,
  input: CarbonSoilProfileInput
): Promise<CarbonSoilProfileRecord> {
  try {
    return toCarbonSoilProfile(
      await apiClient.put<CarbonSoilProfileRequest, CarbonSoilProfileResponse>(
        endpoints.carbon.soilProfiles.byId(soilProfileId),
        toCarbonSoilProfileRequest(input)
      )
    );
  } catch (error) {
    throw toCarbonError(error, "Unable to update Carbon soil profile.");
  }
}

export async function uploadCarbonSoilReport(
  soilProfileId: string,
  file: CarbonSoilReportFile
): Promise<CarbonSoilProfileRecord> {
  try {
    const formData = new FormData();
    await appendUploadFile(formData, "file", file, `soil-report-${Date.now()}.pdf`);

    return toCarbonSoilProfile(
      await apiClient.postFormData<CarbonSoilProfileResponse>(
        endpoints.carbon.soilProfiles.report(soilProfileId),
        formData
      )
    );
  } catch (error) {
    throw toCarbonError(error, "Unable to upload Carbon soil report.");
  }
}

export async function listCarbonActivities(
  profileId: string
): Promise<CarbonActivityRecord[]> {
  try {
    const response = await apiClient.get<CarbonActivityRecordResponse[]>(
      endpoints.carbon.activities.listByProfile(profileId)
    );

    return response.map(toCarbonActivity);
  } catch (error) {
    throw toCarbonError(error, "Unable to load Carbon activities.");
  }
}

export async function createCarbonActivity(
  profileId: string,
  input: CarbonActivityInput
): Promise<CarbonActivityRecord> {
  try {
    return toCarbonActivity(
      await apiClient.post<CarbonActivityRecordRequest, CarbonActivityRecordResponse>(
        endpoints.carbon.activities.createForProfile(profileId),
        toCarbonActivityRequest(input)
      )
    );
  } catch (error) {
    throw toCarbonError(error, "Unable to save Carbon activity.");
  }
}

export async function updateCarbonActivity(
  activityId: string,
  input: CarbonActivityInput
): Promise<CarbonActivityRecord> {
  try {
    return toCarbonActivity(
      await apiClient.put<CarbonActivityRecordRequest, CarbonActivityRecordResponse>(
        endpoints.carbon.activities.byId(activityId),
        toCarbonActivityRequest(input)
      )
    );
  } catch (error) {
    throw toCarbonError(error, "Unable to update Carbon activity.");
  }
}

function toCarbonProfile(response: CarbonProfileResponse): CarbonProfileRecord {
  return {
    aadhaarNumber: response.aadhaarNumber ?? undefined,
    aadhaarStatus: response.aadhaarStatus ?? undefined,
    age: response.age ?? undefined,
    alternateMobileNumber: response.alternateMobileNumber ?? undefined,
    bankStatus: response.bankStatus ?? undefined,
    carbonIdentityId: response.carbonIdentityId,
    coordinatorUserId: response.coordinatorUserId ?? undefined,
    createdAt: response.createdAt,
    croppingPattern: response.croppingPattern ?? undefined,
    displayName: response.displayName,
    districtName: response.districtName ?? undefined,
    documentStatus: response.documentStatus ?? undefined,
    farmerCategory: response.farmerCategory ?? undefined,
    fpoMemberProfileId: response.fpoMemberProfileId ?? undefined,
    gender: response.gender ?? undefined,
    gpsLatitude: response.gpsLatitude ?? undefined,
    gpsLongitude: response.gpsLongitude ?? undefined,
    id: response.id,
    livestockCount: response.livestockCount ?? undefined,
    memberNumber: response.memberNumber ?? undefined,
    mobileNumber: response.mobileNumber ?? undefined,
    participantType: response.participantType,
    stateName: response.stateName ?? undefined,
    status: response.status,
    taluka: response.taluka ?? undefined,
    tenantId: response.tenantId,
    tillageStatus: response.tillageStatus ?? undefined,
    totalLandHoldingAcres: response.totalLandHoldingAcres ?? undefined,
    updatedAt: response.updatedAt,
    userId: response.userId ?? undefined,
    username: response.username ?? undefined,
    village: response.village ?? undefined
  };
}

function toCarbonFarmPlot(response: CarbonFarmPlotResponse): CarbonFarmPlotRecord {
  return {
    areaAcres: response.areaAcres,
    carbonProfileId: response.carbonProfileId,
    createdAt: response.createdAt,
    farmName: response.farmName,
    id: response.id,
    irrigationSource: response.irrigationSource ?? undefined,
    latitude: response.latitude,
    longitude: response.longitude,
    primaryCrop: response.primaryCrop ?? undefined,
    status: response.status,
    surveyNumber: response.surveyNumber ?? undefined,
    tenantId: response.tenantId,
    tillageStatus: response.tillageStatus ?? undefined,
    updatedAt: response.updatedAt
  };
}

function toCarbonSoilProfile(
  response: CarbonSoilProfileResponse
): CarbonSoilProfileRecord {
  return {
    bulkDensityGmCm3: response.bulkDensityGmCm3 ?? undefined,
    carbonFarmPlotId: response.carbonFarmPlotId ?? undefined,
    carbonProfileId: response.carbonProfileId,
    createdAt: response.createdAt,
    electricalConductivity: response.electricalConductivity ?? undefined,
    id: response.id,
    labName: response.labName ?? undefined,
    nitrogenKgHa: response.nitrogenKgHa ?? undefined,
    ph: response.ph ?? undefined,
    phosphorusKgHa: response.phosphorusKgHa ?? undefined,
    potassiumKgHa: response.potassiumKgHa ?? undefined,
    reportContentType: response.reportContentType ?? undefined,
    reportFileName: response.reportFileName ?? undefined,
    reportStorageKey: response.reportStorageKey ?? undefined,
    reportUrl: response.reportUrl ?? undefined,
    soilOrganicCarbonPercent: response.soilOrganicCarbonPercent ?? undefined,
    status: response.status,
    tenantId: response.tenantId,
    testDate: response.testDate ?? undefined,
    texture: response.texture ?? undefined,
    updatedAt: response.updatedAt
  };
}

function toCarbonActivityCategory(
  response: CarbonActivityCategoryResponse
): CarbonActivityCategoryRecord {
  return {
    code: response.code,
    description: response.description,
    evidenceRequired: response.evidenceRequired,
    id: response.id,
    name: response.name,
    sortOrder: response.sortOrder,
    status: response.status
  };
}

function toCarbonActivity(
  response: CarbonActivityRecordResponse
): CarbonActivityRecord {
  return {
    activityDate: response.activityDate,
    carbonFarmPlotId: response.carbonFarmPlotId ?? undefined,
    carbonProfileId: response.carbonProfileId,
    categoryCode: response.categoryCode,
    categoryId: response.categoryId,
    categoryName: response.categoryName,
    cropName: response.cropName,
    evidenceCount: response.evidenceCount,
    evidenceRequired: response.evidenceRequired,
    farmName: response.farmName ?? undefined,
    id: response.id,
    inputUsed: response.inputUsed ?? undefined,
    quantityUnit: response.quantityUnit ?? undefined,
    quantityValue: response.quantityValue ?? undefined,
    remarks: response.remarks ?? undefined,
    status: response.status,
    tenantId: response.tenantId,
    updatedAt: response.updatedAt,
    verificationStatus: response.verificationStatus
  };
}

function toCarbonProfileRequest(input: CarbonProfileInput): CarbonProfileRequest {
  const gpsLatitude = parseOptionalNumber(input.gpsLatitude, "Latitude");
  const gpsLongitude = parseOptionalNumber(input.gpsLongitude, "Longitude");

  if ((gpsLatitude === undefined) !== (gpsLongitude === undefined)) {
    throw new AppError(
      "VALIDATION_FAILED",
      "Both latitude and longitude are required when capturing GPS."
    );
  }

  validateCoordinate(gpsLatitude, -90, 90, "Latitude");
  validateCoordinate(gpsLongitude, -180, 180, "Longitude");

  return removeUndefined({
    aadhaarNumber: cleanOptional(input.aadhaarNumber),
    aadhaarStatus: cleanOptional(input.aadhaarStatus),
    age: parseOptionalInteger(input.age, "Age"),
    alternateMobileNumber: cleanOptional(input.alternateMobileNumber),
    bankStatus: cleanOptional(input.bankStatus),
    carbonIdentityId: cleanOptional(input.carbonIdentityId),
    coordinatorUserId: cleanOptional(input.coordinatorUserId),
    croppingPattern: cleanOptional(input.croppingPattern),
    displayName: requiredText(input.displayName, "Display name"),
    districtName: cleanOptional(input.districtName),
    documentStatus: cleanOptional(input.documentStatus),
    farmerCategory: cleanOptional(input.farmerCategory),
    fpoMemberProfileId: cleanOptional(input.fpoMemberProfileId),
    gender: cleanOptional(input.gender),
    gpsLatitude,
    gpsLongitude,
    livestockCount: parseOptionalInteger(input.livestockCount, "Livestock count"),
    memberNumber: cleanOptional(input.memberNumber),
    mobileNumber: cleanOptional(input.mobileNumber),
    participantType: input.participantType,
    password: input.password || undefined,
    stateName: cleanOptional(input.stateName),
    status: input.status ?? "ACTIVE",
    taluka: cleanOptional(input.taluka),
    tillageStatus: cleanOptional(input.tillageStatus),
    totalLandHoldingAcres: parseOptionalNonNegativeNumber(
      input.totalLandHoldingAcres,
      "Total land holding"
    ),
    userId: cleanOptional(input.userId),
    username: cleanOptional(input.username),
    village: cleanOptional(input.village)
  });
}

function toCarbonFarmPlotRequest(input: CarbonFarmPlotInput): CarbonFarmPlotRequest {
  const latitude = parseRequiredNumber(input.latitude, "Latitude");
  const longitude = parseRequiredNumber(input.longitude, "Longitude");

  validateCoordinate(latitude, -90, 90, "Latitude");
  validateCoordinate(longitude, -180, 180, "Longitude");

  return removeUndefined({
    areaAcres: parsePositiveNumber(input.areaAcres, "Area"),
    farmName: requiredText(input.farmName, "Farm name"),
    irrigationSource: cleanOptional(input.irrigationSource),
    latitude,
    longitude,
    primaryCrop: cleanOptional(input.primaryCrop),
    status: input.status ?? "ACTIVE",
    surveyNumber: cleanOptional(input.surveyNumber),
    tillageStatus: cleanOptional(input.tillageStatus)
  });
}

function toCarbonSoilProfileRequest(
  input: CarbonSoilProfileInput
): CarbonSoilProfileRequest {
  const ph = parseOptionalNonNegativeNumber(input.ph, "pH");
  if (ph !== undefined && ph > 14) {
    throw new AppError("VALIDATION_FAILED", "pH cannot be greater than 14.");
  }

  const reportUrl = cleanOptional(input.reportUrl);
  if (reportUrl && !/^https?:\/\//i.test(reportUrl)) {
    throw new AppError(
      "VALIDATION_FAILED",
      "Report URL must start with http or https."
    );
  }

  return removeUndefined({
    bulkDensityGmCm3: parseOptionalNonNegativeNumber(
      input.bulkDensityGmCm3,
      "Bulk density"
    ),
    carbonFarmPlotId: cleanOptional(input.carbonFarmPlotId),
    electricalConductivity: parseOptionalNonNegativeNumber(
      input.electricalConductivity,
      "EC"
    ),
    labName: cleanOptional(input.labName),
    nitrogenKgHa: parseOptionalNonNegativeNumber(input.nitrogenKgHa, "Nitrogen"),
    ph,
    phosphorusKgHa: parseOptionalNonNegativeNumber(input.phosphorusKgHa, "Phosphorus"),
    potassiumKgHa: parseOptionalNonNegativeNumber(input.potassiumKgHa, "Potassium"),
    reportContentType: cleanOptional(input.reportContentType),
    reportFileName: cleanOptional(input.reportFileName),
    reportStorageKey: cleanOptional(input.reportStorageKey),
    reportUrl,
    soilOrganicCarbonPercent: parseOptionalNonNegativeNumber(
      input.soilOrganicCarbonPercent,
      "SOC"
    ),
    status: input.status ?? "ACTIVE",
    testDate: cleanOptional(input.testDate),
    texture: cleanOptional(input.texture)
  });
}

function toCarbonActivityRequest(
  input: CarbonActivityInput
): CarbonActivityRecordRequest {
  const quantityValue = parseOptionalNonNegativeNumber(input.quantityValue, "Quantity");
  const quantityUnit = cleanOptional(input.quantityUnit);

  if (quantityValue !== undefined && !quantityUnit) {
    throw new AppError(
      "VALIDATION_FAILED",
      "Quantity unit is required when quantity is entered."
    );
  }

  return removeUndefined({
    activityDate: requiredText(input.activityDate, "Activity date"),
    carbonFarmPlotId: cleanOptional(input.carbonFarmPlotId),
    categoryId: requiredText(input.categoryId, "Activity category"),
    cropName: requiredText(input.cropName, "Crop name"),
    inputUsed: cleanOptional(input.inputUsed),
    quantityUnit,
    quantityValue,
    remarks: cleanOptional(input.remarks),
    status: input.status ?? "ACTIVE"
  });
}

function parsePositiveNumber(value: string | undefined, label: string) {
  const parsed = parseRequiredNumber(value, label);
  if (parsed <= 0) {
    throw new AppError("VALIDATION_FAILED", `${label} must be greater than zero.`);
  }

  return parsed;
}

function parseRequiredNumber(value: string | undefined, label: string) {
  const parsed = parseOptionalNumber(value, label);
  if (parsed === undefined) {
    throw new AppError("VALIDATION_FAILED", `${label} is required.`);
  }

  return parsed;
}

function parseOptionalNonNegativeNumber(value: string | undefined, label: string) {
  const parsed = parseOptionalNumber(value, label);
  if (parsed !== undefined && parsed < 0) {
    throw new AppError("VALIDATION_FAILED", `${label} cannot be negative.`);
  }

  return parsed;
}

function parseOptionalInteger(value: string | undefined, label: string) {
  const parsed = parseOptionalNonNegativeNumber(value, label);
  if (parsed !== undefined && !Number.isInteger(parsed)) {
    throw new AppError("VALIDATION_FAILED", `${label} must be a whole number.`);
  }

  return parsed;
}

function parseOptionalNumber(value: string | undefined, label: string) {
  const trimmed = cleanOptional(value);
  if (!trimmed) {
    return undefined;
  }

  const parsed = Number(trimmed);
  if (!Number.isFinite(parsed)) {
    throw new AppError("VALIDATION_FAILED", `${label} must be a valid number.`);
  }

  return parsed;
}

function validateCoordinate(
  value: number | undefined,
  minimum: number,
  maximum: number,
  label: string
) {
  if (value !== undefined && (value < minimum || value > maximum)) {
    throw new AppError(
      "VALIDATION_FAILED",
      `${label} must be between ${minimum} and ${maximum}.`
    );
  }
}

function cleanOptional(value: string | undefined) {
  const trimmed = value?.trim();
  return trimmed || undefined;
}

function requiredText(value: string | undefined, label: string) {
  const trimmed = value?.trim();
  if (!trimmed) {
    throw new AppError("VALIDATION_FAILED", `${label} is required.`);
  }

  return trimmed;
}

function removeUndefined<T extends Record<string, unknown>>(value: T): T {
  return Object.fromEntries(
    Object.entries(value).filter(([, entryValue]) => entryValue !== undefined)
  ) as T;
}

function toCarbonError(error: unknown, fallbackMessage: string) {
  if (error instanceof AppError) {
    return error;
  }

  if (error instanceof ApiClientError) {
    const message =
      error.message === "Unable to complete API request."
        ? fallbackMessage
        : error.message;

    if (error.status === 400) {
      return new AppError("VALIDATION_FAILED", message);
    }

    if (error.status === 401) {
      return new AppError(
        "ACCESS_DENIED",
        "Your login session expired. Please sign in again."
      );
    }

    if (error.status === 403) {
      return new AppError(
        "ACCESS_DENIED",
        "Your role or client module settings do not allow Carbon enrollment access."
      );
    }

    if (error.status === 0) {
      return new AppError(
        "API_REQUEST_FAILED",
        "Unable to reach the backend API. Confirm Spring Boot is running on http://localhost:8080."
      );
    }

    return new AppError("API_REQUEST_FAILED", message);
  }

  return new AppError("API_REQUEST_FAILED", fallbackMessage);
}
