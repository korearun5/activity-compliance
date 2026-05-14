import AsyncStorage from "@react-native-async-storage/async-storage";

import { apiClient, ApiClientError } from "../core/api/client";
import { endpoints } from "../core/api/endpoints";
import {
  CropCatalogRequest,
  CropCatalogResponse,
  CropHistoryRequest,
  CropHistoryResponse,
  CropPlanRequest,
  CropPlanResponse,
  CropPlanStatus,
  CropSeasonRequest,
  CropSeasonResponse,
  FarmRecordStatus,
  UpdateCropPlanStatusRequest,
  UpdateFarmRecordStatusRequest
} from "../core/api/fpoContracts";
import { AppError } from "../core/errors/AppError";
import { logger } from "../core/logging/logger";
import { readJsonArray, writeJson } from "../core/storage/jsonStore";
import { storageKeys } from "../core/storage/storageKeys";

export type CropCatalog = {
  category?: string;
  code: string;
  createdAt: string;
  id: string;
  name: string;
  status: FarmRecordStatus;
  tenantId?: string;
  updatedAt: string;
};

export type CropSeason = {
  code: string;
  createdAt: string;
  endMonth?: number;
  id: string;
  name: string;
  seasonYear: number;
  startMonth?: number;
  status: FarmRecordStatus;
  tenantId?: string;
  updatedAt: string;
};

export type CropHistory = {
  areaAcres?: number;
  createdAt: string;
  cropCode: string;
  cropId: string;
  cropName: string;
  cropYear: number;
  id: string;
  memberId: string;
  memberName: string;
  memberNumber: string;
  notes?: string;
  seasonCode?: string;
  seasonId?: string;
  seasonName?: string;
  tenantId?: string;
  updatedAt: string;
  yieldQuantity?: number;
  yieldUnit?: string;
};

export type CropPlan = {
  confirmedAt?: string;
  createdAt: string;
  cropCode: string;
  cropId: string;
  cropName: string;
  expectedHarvestDate?: string;
  expectedYieldQuintals?: number;
  id: string;
  memberId: string;
  memberName: string;
  memberNumber: string;
  memberVillage: string;
  plannedAreaAcres: number;
  plannedSowingDate?: string;
  plotId?: string;
  plotName?: string;
  seasonCode: string;
  seasonId: string;
  seasonName: string;
  seasonYear: number;
  cropYear: string;
  status: CropPlanStatus;
  tenantId?: string;
  updatedAt: string;
};

export type CropCatalogInput = {
  category?: string;
  code: string;
  name: string;
  status?: FarmRecordStatus;
};

export type CropSeasonInput = {
  code: string;
  endMonth?: string;
  name: string;
  seasonYear: string;
  startMonth?: string;
  status?: FarmRecordStatus;
};

export type CropHistoryInput = {
  areaAcres?: string;
  cropId: string;
  cropYear: string;
  notes?: string;
  seasonId?: string;
  yieldQuantity?: string;
  yieldUnit?: string;
};

export type CropPlanInput = {
  cropId: string;
  cropYear: string;
  expectedHarvestDate?: string;
  expectedYieldQuintals?: string;
  memberId: string;
  plannedAreaAcres: string;
  plannedSowingDate?: string;
  plotId?: string;
  seasonId: string;
  status?: CropPlanStatus;
};

export type CropPlanFilters = {
  cropId?: string;
  memberId?: string;
  seasonId?: string;
  status?: CropPlanStatus;
};

export async function getCropCatalog(): Promise<CropCatalog[]> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.get<CropCatalogResponse[]>(
        endpoints.fpo.crops.list,
        { accessToken }
      );
      const crops = response.map(toCropCatalog);

      await writeJson(storageKeys.fpo.crops, crops);
      return crops;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toCropPlanningError(error);
      }

      logger.warn("Backend crop catalog unavailable; using cached crop records.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return getLocalCropCatalog();
}

export async function createCropCatalog(
  input: CropCatalogInput
): Promise<CropCatalog> {
  const request = toCropCatalogRequest(input);
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.post<CropCatalogRequest, CropCatalogResponse>(
        endpoints.fpo.crops.create,
        request,
        { accessToken }
      );
      const crop = toCropCatalog(response);

      await upsertLocalCrop(crop);
      return crop;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toCropPlanningError(error);
      }

      logger.warn("Backend crop creation unavailable; using cached crop records.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  await ensureLocalCropCodeAvailable(request.code);
  return upsertLocalCrop({
    category: request.category,
    code: request.code,
    createdAt: new Date().toISOString(),
    id: `local-crop-${Date.now()}`,
    name: request.name,
    status: request.status ?? "ACTIVE",
    updatedAt: new Date().toISOString()
  });
}

export async function updateCropStatus(
  crop: CropCatalog,
  status: FarmRecordStatus
) {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.patch<
        UpdateFarmRecordStatusRequest,
        CropCatalogResponse
      >(endpoints.fpo.crops.status(crop.id), { status }, { accessToken });
      const updated = toCropCatalog(response);

      await upsertLocalCrop(updated);
      return updated;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toCropPlanningError(error);
      }

      logger.warn("Backend crop status unavailable; using cached crop records.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return upsertLocalCrop({ ...crop, status, updatedAt: new Date().toISOString() });
}

export async function getCropSeasons(): Promise<CropSeason[]> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.get<CropSeasonResponse[]>(
        endpoints.fpo.seasons.list,
        { accessToken }
      );
      const seasons = response.map(toCropSeason);

      await writeJson(storageKeys.fpo.seasons, seasons);
      return seasons;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toCropPlanningError(error);
      }

      logger.warn("Backend crop season list unavailable; using cached seasons.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return getLocalCropSeasons();
}

export async function createCropSeason(input: CropSeasonInput): Promise<CropSeason> {
  const request = toCropSeasonRequest(input);
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.post<CropSeasonRequest, CropSeasonResponse>(
        endpoints.fpo.seasons.create,
        request,
        { accessToken }
      );
      const season = toCropSeason(response);

      await upsertLocalCropSeason(season);
      return season;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toCropPlanningError(error);
      }

      logger.warn("Backend season creation unavailable; using cached seasons.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  await ensureLocalSeasonCodeAvailable(request.code, request.seasonYear);
  return upsertLocalCropSeason({
    code: request.code,
    createdAt: new Date().toISOString(),
    endMonth: request.endMonth,
    id: `local-season-${Date.now()}`,
    name: request.name,
    seasonYear: request.seasonYear,
    startMonth: request.startMonth,
    status: request.status ?? "ACTIVE",
    updatedAt: new Date().toISOString()
  });
}

export async function updateCropSeasonStatus(
  season: CropSeason,
  status: FarmRecordStatus
) {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.patch<
        UpdateFarmRecordStatusRequest,
        CropSeasonResponse
      >(endpoints.fpo.seasons.status(season.id), { status }, { accessToken });
      const updated = toCropSeason(response);

      await upsertLocalCropSeason(updated);
      return updated;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toCropPlanningError(error);
      }

      logger.warn("Backend season status unavailable; using cached seasons.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return upsertLocalCropSeason({
    ...season,
    status,
    updatedAt: new Date().toISOString()
  });
}

export async function getCropHistory(memberId: string): Promise<CropHistory[]> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.get<CropHistoryResponse[]>(
        endpoints.fpo.cropHistory.listByMember(memberId),
        { accessToken }
      );
      const history = response.map(toCropHistory);

      await replaceLocalCropHistory(memberId, history);
      return history;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toCropPlanningError(error);
      }

      logger.warn("Backend crop history unavailable; using cached history.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return getLocalCropHistory(memberId);
}

export async function createCropHistory(
  memberId: string,
  memberNumber: string,
  memberName: string,
  input: CropHistoryInput,
  crops: CropCatalog[],
  seasons: CropSeason[]
) {
  const request = toCropHistoryRequest(input);
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.post<CropHistoryRequest, CropHistoryResponse>(
        endpoints.fpo.cropHistory.createForMember(memberId),
        request,
        { accessToken }
      );
      const history = toCropHistory(response);

      await upsertLocalCropHistory(history);
      return history;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toCropPlanningError(error);
      }

      logger.warn("Backend crop history creation unavailable; using cached history.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  const crop = crops.find((item) => item.id === request.cropId);
  const season = seasons.find((item) => item.id === request.seasonId);
  if (!crop) {
    throw new AppError("VALIDATION_FAILED", "Select a valid crop.");
  }

  return upsertLocalCropHistory({
    areaAcres: request.areaAcres,
    createdAt: new Date().toISOString(),
    cropCode: crop.code,
    cropId: crop.id,
    cropName: crop.name,
    cropYear: request.cropYear,
    id: `local-crop-history-${Date.now()}`,
    memberId,
    memberName,
    memberNumber,
    notes: request.notes,
    seasonCode: season?.code,
    seasonId: season?.id,
    seasonName: season?.name,
    updatedAt: new Date().toISOString(),
    yieldQuantity: request.yieldQuantity,
    yieldUnit: request.yieldUnit
  });
}

export async function getCropPlans(
  filters: CropPlanFilters = {}
): Promise<CropPlan[]> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.get<CropPlanResponse[]>(
        cropPlanListEndpoint(filters),
        { accessToken }
      );
      const plans = response.map(toCropPlan);

      await writeJson(storageKeys.fpo.cropPlans, plans);
      return plans;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toCropPlanningError(error);
      }

      logger.warn("Backend crop plans unavailable; using cached crop plans.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  const plans = await getLocalCropPlans();
  return plans.filter((plan) => matchesCropPlanFilters(plan, filters));
}

export async function createCropPlan(
  input: CropPlanInput,
  membersById: Map<string, { memberNumber: string; name: string; village: string }>,
  crops: CropCatalog[],
  seasons: CropSeason[]
) {
  const request = toCropPlanRequest(input);
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.post<CropPlanRequest, CropPlanResponse>(
        endpoints.fpo.cropPlans.create,
        request,
        { accessToken }
      );
      const plan = toCropPlan(response);

      await upsertLocalCropPlan(plan);
      return plan;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toCropPlanningError(error);
      }

      logger.warn("Backend crop plan creation unavailable; using cached crop plans.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  const member = membersById.get(request.memberId);
  const crop = crops.find((item) => item.id === request.cropId);
  const season = seasons.find((item) => item.id === request.seasonId);
  if (!member || !crop || !season) {
    throw new AppError("VALIDATION_FAILED", "Select a valid member, crop, and season.");
  }

  return upsertLocalCropPlan({
    confirmedAt:
      (request.status ?? "DRAFT") === "CONFIRMED" ? new Date().toISOString() : undefined,
    createdAt: new Date().toISOString(),
    cropCode: crop.code,
    cropId: crop.id,
    cropName: crop.name,
    expectedHarvestDate: request.expectedHarvestDate,
    expectedYieldQuintals: request.expectedYieldQuintals,
    id: `local-crop-plan-${Date.now()}`,
    memberId: request.memberId,
    memberName: member.name,
    memberNumber: member.memberNumber,
    memberVillage: member.village,
    plannedAreaAcres: request.plannedAreaAcres,
    plannedSowingDate: request.plannedSowingDate,
    plotId: request.plotId,
    seasonCode: season.code,
    seasonId: season.id,
    seasonName: season.name,
    seasonYear: season.seasonYear,
    cropYear: request.cropYear,
    status: request.status ?? "DRAFT",
    updatedAt: new Date().toISOString()
  });
}

export async function updateCropPlanStatus(
  plan: CropPlan,
  status: CropPlanStatus
) {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.patch<
        UpdateCropPlanStatusRequest,
        CropPlanResponse
      >(endpoints.fpo.cropPlans.status(plan.id), { status }, { accessToken });
      const updated = toCropPlan(response);

      await upsertLocalCropPlan(updated);
      return updated;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toCropPlanningError(error);
      }

      logger.warn("Backend crop plan status unavailable; using cached crop plans.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return upsertLocalCropPlan({
    ...plan,
    confirmedAt:
      status === "CONFIRMED" && plan.status !== "CONFIRMED"
        ? new Date().toISOString()
        : plan.confirmedAt,
    status,
    updatedAt: new Date().toISOString()
  });
}

function toCropCatalog(response: CropCatalogResponse): CropCatalog {
  return {
    category: response.category ?? undefined,
    code: response.code,
    createdAt: response.createdAt,
    id: response.id,
    name: response.name,
    status: response.status,
    tenantId: response.tenantId,
    updatedAt: response.updatedAt
  };
}

function toCropSeason(response: CropSeasonResponse): CropSeason {
  return {
    code: response.code,
    createdAt: response.createdAt,
    endMonth: response.endMonth ?? undefined,
    id: response.id,
    name: response.name,
    seasonYear: response.seasonYear,
    startMonth: response.startMonth ?? undefined,
    status: response.status,
    tenantId: response.tenantId,
    updatedAt: response.updatedAt
  };
}

function toCropHistory(response: CropHistoryResponse): CropHistory {
  return {
    areaAcres: response.areaAcres ?? undefined,
    createdAt: response.createdAt,
    cropCode: response.cropCode,
    cropId: response.cropId,
    cropName: response.cropName,
    cropYear: response.cropYear,
    id: response.id,
    memberId: response.memberId,
    memberName: response.memberName,
    memberNumber: response.memberNumber,
    notes: response.notes ?? undefined,
    seasonCode: response.seasonCode ?? undefined,
    seasonId: response.seasonId ?? undefined,
    seasonName: response.seasonName ?? undefined,
    tenantId: response.tenantId,
    updatedAt: response.updatedAt,
    yieldQuantity: response.yieldQuantity ?? undefined,
    yieldUnit: response.yieldUnit ?? undefined
  };
}

function toCropPlan(response: CropPlanResponse): CropPlan {
  return {
    confirmedAt: response.confirmedAt ?? undefined,
    createdAt: response.createdAt,
    cropCode: response.cropCode,
    cropId: response.cropId,
    cropName: response.cropName,
    expectedHarvestDate: response.expectedHarvestDate ?? undefined,
    expectedYieldQuintals: response.expectedYieldQuintals ?? undefined,
    id: response.id,
    memberId: response.memberId,
    memberName: response.memberName,
    memberNumber: response.memberNumber,
    memberVillage: response.memberVillage,
    plannedAreaAcres: response.plannedAreaAcres,
    plannedSowingDate: response.plannedSowingDate ?? undefined,
    plotId: response.plotId ?? undefined,
    plotName: response.plotName ?? undefined,
    seasonCode: response.seasonCode,
    seasonId: response.seasonId,
    seasonName: response.seasonName,
    seasonYear: response.seasonYear,
    cropYear: response.cropYear,
    status: response.status,
    tenantId: response.tenantId,
    updatedAt: response.updatedAt
  };
}

function toCropCatalogRequest(input: CropCatalogInput): CropCatalogRequest {
  if (!input.code.trim() || !input.name.trim()) {
    throw new AppError("VALIDATION_FAILED", "Enter crop code and name.");
  }

  return {
    category: cleanOptional(input.category),
    code: input.code.trim().toUpperCase(),
    name: input.name.trim(),
    status: input.status ?? "ACTIVE"
  };
}

function toCropSeasonRequest(input: CropSeasonInput): CropSeasonRequest {
  const seasonYear = parseWholeNumber(input.seasonYear, "Season year");
  const startMonth = parseOptionalWholeNumber(input.startMonth, "Start month");
  const endMonth = parseOptionalWholeNumber(input.endMonth, "End month");

  if (!input.code.trim() || !input.name.trim()) {
    throw new AppError("VALIDATION_FAILED", "Enter season code and name.");
  }

  if ((startMonth === undefined) !== (endMonth === undefined)) {
    throw new AppError(
      "VALIDATION_FAILED",
      "Enter both start and end month, or leave both blank."
    );
  }

  if (
    startMonth !== undefined &&
    (startMonth < 1 || startMonth > 12 || endMonth === undefined || endMonth < 1 || endMonth > 12)
  ) {
    throw new AppError("VALIDATION_FAILED", "Season months must be between 1 and 12.");
  }

  if (seasonYear < 1900 || seasonYear > 2200) {
    throw new AppError("VALIDATION_FAILED", "Season year must be between 1900 and 2200.");
  }

  return {
    code: input.code.trim().toUpperCase(),
    endMonth,
    name: input.name.trim(),
    seasonYear,
    startMonth,
    status: input.status ?? "ACTIVE"
  };
}

function toCropHistoryRequest(input: CropHistoryInput): CropHistoryRequest {
  if (!input.cropId) {
    throw new AppError("VALIDATION_FAILED", "Select a crop.");
  }

  const cropYear = parseWholeNumber(input.cropYear, "Crop year");
  if (cropYear < 1900 || cropYear > 2200) {
    throw new AppError("VALIDATION_FAILED", "Crop year must be between 1900 and 2200.");
  }

  const yieldQuantity = parseOptionalNonNegativeNumber(
    input.yieldQuantity,
    "Yield quantity"
  );
  const yieldUnit = cleanOptional(input.yieldUnit);
  if (yieldQuantity !== undefined && !yieldUnit) {
    throw new AppError(
      "VALIDATION_FAILED",
      "Yield unit is required when yield quantity is entered."
    );
  }

  return {
    areaAcres: parseOptionalPositiveNumber(input.areaAcres, "Crop area"),
    cropId: input.cropId,
    cropYear,
    notes: cleanOptional(input.notes),
    seasonId: cleanOptional(input.seasonId),
    yieldQuantity,
    yieldUnit
  };
}

function toCropPlanRequest(input: CropPlanInput): CropPlanRequest {
  if (!input.memberId || !input.cropId || !input.seasonId) {
    throw new AppError("VALIDATION_FAILED", "Select member, crop, and season.");
  }

  const cropYear = cleanOptional(input.cropYear);
  if (!cropYear) {
    throw new AppError("VALIDATION_FAILED", "Enter crop year.");
  }

  const plannedSowingDate = cleanOptional(input.plannedSowingDate);
  const expectedHarvestDate = cleanOptional(input.expectedHarvestDate);
  if (
    plannedSowingDate &&
    expectedHarvestDate &&
    expectedHarvestDate < plannedSowingDate
  ) {
    throw new AppError(
      "VALIDATION_FAILED",
      "Expected harvest date cannot be before sowing date."
    );
  }

  return {
    cropId: input.cropId,
    cropYear,
    expectedHarvestDate,
    expectedYieldQuintals: parseOptionalNonNegativeNumber(
      input.expectedYieldQuintals,
      "Expected yield"
    ),
    memberId: input.memberId,
    plannedAreaAcres: parsePositiveNumber(input.plannedAreaAcres, "Planned area"),
    plannedSowingDate,
    plotId: cleanOptional(input.plotId),
    seasonId: input.seasonId,
    status: input.status ?? "DRAFT"
  };
}

function toStoredCropCatalog(crop: Partial<CropCatalog>): CropCatalog | null {
  if (
    typeof crop.id !== "string" ||
    typeof crop.code !== "string" ||
    typeof crop.name !== "string" ||
    typeof crop.createdAt !== "string" ||
    typeof crop.updatedAt !== "string"
  ) {
    return null;
  }

  return {
    category: crop.category,
    code: crop.code,
    createdAt: crop.createdAt,
    id: crop.id,
    name: crop.name,
    status: crop.status ?? "ACTIVE",
    tenantId: crop.tenantId,
    updatedAt: crop.updatedAt
  };
}

function toStoredCropSeason(season: Partial<CropSeason>): CropSeason | null {
  if (
    typeof season.id !== "string" ||
    typeof season.code !== "string" ||
    typeof season.name !== "string" ||
    typeof season.seasonYear !== "number" ||
    typeof season.createdAt !== "string" ||
    typeof season.updatedAt !== "string"
  ) {
    return null;
  }

  return {
    code: season.code,
    createdAt: season.createdAt,
    endMonth: season.endMonth,
    id: season.id,
    name: season.name,
    seasonYear: season.seasonYear,
    startMonth: season.startMonth,
    status: season.status ?? "ACTIVE",
    tenantId: season.tenantId,
    updatedAt: season.updatedAt
  };
}

function toStoredCropHistory(history: Partial<CropHistory>): CropHistory | null {
  if (
    typeof history.id !== "string" ||
    typeof history.memberId !== "string" ||
    typeof history.memberNumber !== "string" ||
    typeof history.memberName !== "string" ||
    typeof history.cropId !== "string" ||
    typeof history.cropCode !== "string" ||
    typeof history.cropName !== "string" ||
    typeof history.cropYear !== "number" ||
    typeof history.createdAt !== "string" ||
    typeof history.updatedAt !== "string"
  ) {
    return null;
  }

  return {
    areaAcres: history.areaAcres,
    createdAt: history.createdAt,
    cropCode: history.cropCode,
    cropId: history.cropId,
    cropName: history.cropName,
    cropYear: history.cropYear,
    id: history.id,
    memberId: history.memberId,
    memberName: history.memberName,
    memberNumber: history.memberNumber,
    notes: history.notes,
    seasonCode: history.seasonCode,
    seasonId: history.seasonId,
    seasonName: history.seasonName,
    tenantId: history.tenantId,
    updatedAt: history.updatedAt,
    yieldQuantity: history.yieldQuantity,
    yieldUnit: history.yieldUnit
  };
}

function toStoredCropPlan(plan: Partial<CropPlan>): CropPlan | null {
  if (
    typeof plan.id !== "string" ||
    typeof plan.memberId !== "string" ||
    typeof plan.memberName !== "string" ||
    typeof plan.memberNumber !== "string" ||
    typeof plan.memberVillage !== "string" ||
    typeof plan.cropId !== "string" ||
    typeof plan.cropCode !== "string" ||
    typeof plan.cropName !== "string" ||
    typeof plan.seasonId !== "string" ||
    typeof plan.seasonCode !== "string" ||
    typeof plan.seasonName !== "string" ||
    typeof plan.seasonYear !== "number" ||
    typeof plan.cropYear !== "string" ||
    typeof plan.plannedAreaAcres !== "number" ||
    typeof plan.createdAt !== "string" ||
    typeof plan.updatedAt !== "string"
  ) {
    return null;
  }

  return {
    confirmedAt: plan.confirmedAt,
    createdAt: plan.createdAt,
    cropCode: plan.cropCode,
    cropId: plan.cropId,
    cropName: plan.cropName,
    expectedHarvestDate: plan.expectedHarvestDate,
    expectedYieldQuintals: plan.expectedYieldQuintals,
    id: plan.id,
    memberId: plan.memberId,
    memberName: plan.memberName,
    memberNumber: plan.memberNumber,
    memberVillage: plan.memberVillage,
    plannedAreaAcres: plan.plannedAreaAcres,
    plannedSowingDate: plan.plannedSowingDate,
    plotId: plan.plotId,
    plotName: plan.plotName,
    seasonCode: plan.seasonCode,
    seasonId: plan.seasonId,
    seasonName: plan.seasonName,
    seasonYear: plan.seasonYear,
    cropYear: plan.cropYear,
    status: plan.status ?? "DRAFT",
    tenantId: plan.tenantId,
    updatedAt: plan.updatedAt
  };
}

async function getLocalCropCatalog() {
  const saved = await readJsonArray<Partial<CropCatalog>>([storageKeys.fpo.crops]);
  return saved
    .map(toStoredCropCatalog)
    .filter((crop): crop is CropCatalog => Boolean(crop));
}

async function upsertLocalCrop(crop: CropCatalog) {
  const current = await getLocalCropCatalog();
  await writeJson(storageKeys.fpo.crops, [
    crop,
    ...current.filter((item) => item.id !== crop.id)
  ]);
  return crop;
}

async function ensureLocalCropCodeAvailable(code: string) {
  const crops = await getLocalCropCatalog();
  if (crops.some((crop) => crop.code.toLowerCase() === code.toLowerCase())) {
    throw new AppError("DUPLICATE_RESOURCE", "Crop code already exists.");
  }
}

async function getLocalCropSeasons() {
  const saved = await readJsonArray<Partial<CropSeason>>([
    storageKeys.fpo.seasons
  ]);
  return saved
    .map(toStoredCropSeason)
    .filter((season): season is CropSeason => Boolean(season));
}

async function upsertLocalCropSeason(season: CropSeason) {
  const current = await getLocalCropSeasons();
  await writeJson(storageKeys.fpo.seasons, [
    season,
    ...current.filter((item) => item.id !== season.id)
  ]);
  return season;
}

async function ensureLocalSeasonCodeAvailable(code: string, seasonYear: number) {
  const seasons = await getLocalCropSeasons();
  if (
    seasons.some(
      (season) =>
        season.seasonYear === seasonYear &&
        season.code.toLowerCase() === code.toLowerCase()
    )
  ) {
    throw new AppError(
      "DUPLICATE_RESOURCE",
      "Season code already exists for this year."
    );
  }
}

async function getLocalCropHistory(memberId: string) {
  const saved = await readJsonArray<Partial<CropHistory>>([
    storageKeys.fpo.cropHistory
  ]);
  return saved
    .map(toStoredCropHistory)
    .filter((history): history is CropHistory => Boolean(history))
    .filter((history) => history.memberId === memberId);
}

async function replaceLocalCropHistory(memberId: string, history: CropHistory[]) {
  const saved = await readJsonArray<Partial<CropHistory>>([
    storageKeys.fpo.cropHistory
  ]);
  const current = saved
    .map(toStoredCropHistory)
    .filter((item): item is CropHistory => Boolean(item))
    .filter((item) => item.memberId !== memberId);

  await writeJson(storageKeys.fpo.cropHistory, [...history, ...current]);
}

async function upsertLocalCropHistory(history: CropHistory) {
  const saved = await readJsonArray<Partial<CropHistory>>([
    storageKeys.fpo.cropHistory
  ]);
  const current = saved
    .map(toStoredCropHistory)
    .filter((item): item is CropHistory => Boolean(item));

  await writeJson(storageKeys.fpo.cropHistory, [
    history,
    ...current.filter((item) => item.id !== history.id)
  ]);
  return history;
}

async function getLocalCropPlans() {
  const saved = await readJsonArray<Partial<CropPlan>>([
    storageKeys.fpo.cropPlans
  ]);
  return saved
    .map(toStoredCropPlan)
    .filter((plan): plan is CropPlan => Boolean(plan));
}

async function upsertLocalCropPlan(plan: CropPlan) {
  const current = await getLocalCropPlans();
  await writeJson(storageKeys.fpo.cropPlans, [
    plan,
    ...current.filter((item) => item.id !== plan.id)
  ]);
  return plan;
}

function cropPlanListEndpoint(filters: CropPlanFilters) {
  const params = new URLSearchParams();
  if (filters.memberId) params.append("memberId", filters.memberId);
  if (filters.cropId) params.append("cropId", filters.cropId);
  if (filters.seasonId) params.append("seasonId", filters.seasonId);
  if (filters.status) params.append("status", filters.status);
  const query = params.toString();
  return query ? `${endpoints.fpo.cropPlans.list}?${query}` : endpoints.fpo.cropPlans.list;
}

function matchesCropPlanFilters(plan: CropPlan, filters: CropPlanFilters) {
  return (
    (!filters.memberId || plan.memberId === filters.memberId) &&
    (!filters.cropId || plan.cropId === filters.cropId) &&
    (!filters.seasonId || plan.seasonId === filters.seasonId) &&
    (!filters.status || plan.status === filters.status)
  );
}

function parsePositiveNumber(value: string, label: string) {
  const parsed = parseOptionalNumber(value, label);
  if (parsed === undefined || parsed <= 0) {
    throw new AppError("VALIDATION_FAILED", `${label} must be greater than zero.`);
  }

  return parsed;
}

function parseOptionalPositiveNumber(value: string | undefined, label: string) {
  const parsed = parseOptionalNumber(value, label);
  if (parsed !== undefined && parsed <= 0) {
    throw new AppError("VALIDATION_FAILED", `${label} must be greater than zero.`);
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

function parseOptionalNumber(value: string | undefined, label: string) {
  const trimmed = value?.trim();
  if (!trimmed) {
    return undefined;
  }

  const parsed = Number(trimmed);
  if (!Number.isFinite(parsed)) {
    throw new AppError("VALIDATION_FAILED", `${label} must be a valid number.`);
  }

  return parsed;
}

function parseWholeNumber(value: string, label: string) {
  const parsed = parseOptionalWholeNumber(value, label);
  if (parsed === undefined) {
    throw new AppError("VALIDATION_FAILED", `${label} is required.`);
  }

  return parsed;
}

function parseOptionalWholeNumber(value: string | undefined, label: string) {
  const parsed = parseOptionalNumber(value, label);
  if (parsed !== undefined && !Number.isInteger(parsed)) {
    throw new AppError("VALIDATION_FAILED", `${label} must be a whole number.`);
  }

  return parsed;
}

function cleanOptional(value: string | undefined) {
  const trimmed = value?.trim();
  return trimmed || undefined;
}

async function getAccessToken() {
  return AsyncStorage.getItem(storageKeys.auth.accessToken);
}

function canUseLocalFallback(error: unknown) {
  return !(error instanceof ApiClientError);
}

function toCropPlanningError(error: unknown) {
  if (error instanceof ApiClientError) {
    if (error.status === 409) {
      return new AppError("DUPLICATE_RESOURCE", error.message);
    }

    if (error.status === 400) {
      return new AppError("VALIDATION_FAILED", error.message);
    }

    if (error.status === 401 || error.status === 403) {
      return new AppError("ACCESS_DENIED", error.message);
    }

    return new AppError("API_REQUEST_FAILED", error.message);
  }

  return new AppError("API_REQUEST_FAILED", "Unable to manage crop planning.");
}
