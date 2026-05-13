import AsyncStorage from "@react-native-async-storage/async-storage";

import { apiClient, ApiClientError } from "../core/api/client";
import { endpoints } from "../core/api/endpoints";
import {
  CreateFarmLandholdingRequest,
  CreateFarmPlotRequest,
  FarmLandholdingResponse,
  FarmPlotResponse,
  FarmRecordStatus,
  UpdateFarmRecordStatusRequest
} from "../core/api/fpoContracts";
import { AppError } from "../core/errors/AppError";
import { logger } from "../core/logging/logger";
import { readJsonArray, writeJson } from "../core/storage/jsonStore";
import { storageKeys } from "../core/storage/storageKeys";

export type FarmLandholding = {
  createdAt: string;
  cultivableAreaAcres?: number;
  id: string;
  irrigationSource?: string;
  memberId: string;
  memberNumber: string;
  ownershipType?: string;
  status: FarmRecordStatus;
  surveyNumber?: string;
  tenantId?: string;
  totalAreaAcres: number;
  updatedAt: string;
};

export type FarmPlot = {
  areaAcres: number;
  createdAt: string;
  id: string;
  landholdingId?: string;
  latitude?: number;
  longitude?: number;
  memberId: string;
  memberNumber: string;
  plotName: string;
  soilType?: string;
  status: FarmRecordStatus;
  tenantId?: string;
  updatedAt: string;
};

export type FarmLandholdingInput = {
  cultivableAreaAcres?: string;
  irrigationSource?: string;
  ownershipType?: string;
  status?: FarmRecordStatus;
  surveyNumber?: string;
  totalAreaAcres: string;
};

export type FarmPlotInput = {
  areaAcres: string;
  landholdingId?: string;
  latitude?: string;
  longitude?: string;
  plotName: string;
  soilType?: string;
  status?: FarmRecordStatus;
};

export async function getFarmLandholdings(
  memberId: string
): Promise<FarmLandholding[]> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.get<FarmLandholdingResponse[]>(
        endpoints.fpo.landholdings.listByMember(memberId),
        { accessToken }
      );
      const landholdings = response.map(toFarmLandholding);

      await replaceLocalFarmLandholdings(memberId, landholdings);
      return landholdings;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toFarmAssetError(error);
      }

      logger.warn("Backend landholding list unavailable; using cached records.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return getLocalFarmLandholdings(memberId);
}

export async function createFarmLandholding(
  memberId: string,
  memberNumber: string,
  input: FarmLandholdingInput
): Promise<FarmLandholding> {
  const request = toLandholdingRequest(input);
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.post<
        CreateFarmLandholdingRequest,
        FarmLandholdingResponse
      >(endpoints.fpo.landholdings.createForMember(memberId), request, {
        accessToken
      });
      const landholding = toFarmLandholding(response);

      await upsertLocalFarmLandholding(landholding);
      return landholding;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toFarmAssetError(error);
      }

      logger.warn("Backend landholding creation unavailable; using cached records.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return upsertLocalFarmLandholding({
    createdAt: new Date().toISOString(),
    cultivableAreaAcres: request.cultivableAreaAcres,
    id: `local-landholding-${Date.now()}`,
    irrigationSource: request.irrigationSource,
    memberId,
    memberNumber,
    ownershipType: request.ownershipType,
    status: request.status ?? "ACTIVE",
    surveyNumber: request.surveyNumber,
    totalAreaAcres: request.totalAreaAcres,
    updatedAt: new Date().toISOString()
  });
}

export async function updateFarmLandholdingStatus(
  landholding: FarmLandholding,
  status: FarmRecordStatus
) {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.patch<
        UpdateFarmRecordStatusRequest,
        FarmLandholdingResponse
      >(
        endpoints.fpo.landholdings.status(landholding.id),
        { status },
        { accessToken }
      );
      const updated = toFarmLandholding(response);

      await upsertLocalFarmLandholding(updated);
      return updated;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toFarmAssetError(error);
      }

      logger.warn("Backend landholding status unavailable; using cached records.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return upsertLocalFarmLandholding({
    ...landholding,
    status,
    updatedAt: new Date().toISOString()
  });
}

export async function getFarmPlots(memberId: string): Promise<FarmPlot[]> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.get<FarmPlotResponse[]>(
        endpoints.fpo.plots.listByMember(memberId),
        { accessToken }
      );
      const plots = response.map(toFarmPlot);

      await replaceLocalFarmPlots(memberId, plots);
      return plots;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toFarmAssetError(error);
      }

      logger.warn("Backend plot list unavailable; using cached records.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return getLocalFarmPlots(memberId);
}

export async function createFarmPlot(
  memberId: string,
  memberNumber: string,
  input: FarmPlotInput
): Promise<FarmPlot> {
  const request = toPlotRequest(input);
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.post<CreateFarmPlotRequest, FarmPlotResponse>(
        endpoints.fpo.plots.createForMember(memberId),
        request,
        { accessToken }
      );
      const plot = toFarmPlot(response);

      await upsertLocalFarmPlot(plot);
      return plot;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toFarmAssetError(error);
      }

      logger.warn("Backend plot creation unavailable; using cached records.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return upsertLocalFarmPlot({
    areaAcres: request.areaAcres,
    createdAt: new Date().toISOString(),
    id: `local-plot-${Date.now()}`,
    landholdingId: request.landholdingId,
    latitude: request.latitude,
    longitude: request.longitude,
    memberId,
    memberNumber,
    plotName: request.plotName,
    soilType: request.soilType,
    status: request.status ?? "ACTIVE",
    updatedAt: new Date().toISOString()
  });
}

export async function updateFarmPlotStatus(plot: FarmPlot, status: FarmRecordStatus) {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.patch<
        UpdateFarmRecordStatusRequest,
        FarmPlotResponse
      >(endpoints.fpo.plots.status(plot.id), { status }, { accessToken });
      const updated = toFarmPlot(response);

      await upsertLocalFarmPlot(updated);
      return updated;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toFarmAssetError(error);
      }

      logger.warn("Backend plot status unavailable; using cached records.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return upsertLocalFarmPlot({
    ...plot,
    status,
    updatedAt: new Date().toISOString()
  });
}

function toFarmLandholding(response: FarmLandholdingResponse): FarmLandholding {
  return {
    createdAt: response.createdAt,
    cultivableAreaAcres: response.cultivableAreaAcres ?? undefined,
    id: response.id,
    irrigationSource: response.irrigationSource ?? undefined,
    memberId: response.memberId,
    memberNumber: response.memberNumber,
    ownershipType: response.ownershipType ?? undefined,
    status: response.status,
    surveyNumber: response.surveyNumber ?? undefined,
    tenantId: response.tenantId,
    totalAreaAcres: response.totalAreaAcres,
    updatedAt: response.updatedAt
  };
}

function toFarmPlot(response: FarmPlotResponse): FarmPlot {
  return {
    areaAcres: response.areaAcres,
    createdAt: response.createdAt,
    id: response.id,
    landholdingId: response.landholdingId ?? undefined,
    latitude: response.latitude ?? undefined,
    longitude: response.longitude ?? undefined,
    memberId: response.memberId,
    memberNumber: response.memberNumber,
    plotName: response.plotName,
    soilType: response.soilType ?? undefined,
    status: response.status,
    tenantId: response.tenantId,
    updatedAt: response.updatedAt
  };
}

function toStoredFarmLandholding(
  landholding: Partial<FarmLandholding>
): FarmLandholding | null {
  if (
    typeof landholding.id !== "string" ||
    typeof landholding.memberId !== "string" ||
    typeof landholding.memberNumber !== "string" ||
    typeof landholding.totalAreaAcres !== "number" ||
    typeof landholding.createdAt !== "string" ||
    typeof landholding.updatedAt !== "string"
  ) {
    return null;
  }

  return {
    createdAt: landholding.createdAt,
    cultivableAreaAcres: landholding.cultivableAreaAcres,
    id: landholding.id,
    irrigationSource: landholding.irrigationSource,
    memberId: landholding.memberId,
    memberNumber: landholding.memberNumber,
    ownershipType: landholding.ownershipType,
    status: landholding.status ?? "ACTIVE",
    surveyNumber: landholding.surveyNumber,
    tenantId: landholding.tenantId,
    totalAreaAcres: landholding.totalAreaAcres,
    updatedAt: landholding.updatedAt
  };
}

function toStoredFarmPlot(plot: Partial<FarmPlot>): FarmPlot | null {
  if (
    typeof plot.id !== "string" ||
    typeof plot.memberId !== "string" ||
    typeof plot.memberNumber !== "string" ||
    typeof plot.plotName !== "string" ||
    typeof plot.areaAcres !== "number" ||
    typeof plot.createdAt !== "string" ||
    typeof plot.updatedAt !== "string"
  ) {
    return null;
  }

  return {
    areaAcres: plot.areaAcres,
    createdAt: plot.createdAt,
    id: plot.id,
    landholdingId: plot.landholdingId,
    latitude: plot.latitude,
    longitude: plot.longitude,
    memberId: plot.memberId,
    memberNumber: plot.memberNumber,
    plotName: plot.plotName,
    soilType: plot.soilType,
    status: plot.status ?? "ACTIVE",
    tenantId: plot.tenantId,
    updatedAt: plot.updatedAt
  };
}

function toLandholdingRequest(
  input: FarmLandholdingInput
): CreateFarmLandholdingRequest {
  const totalAreaAcres = parsePositiveNumber(input.totalAreaAcres, "Total area");
  const cultivableAreaAcres = parseOptionalNonNegativeNumber(
    input.cultivableAreaAcres,
    "Cultivable area"
  );

  if (
    cultivableAreaAcres !== undefined &&
    cultivableAreaAcres > totalAreaAcres
  ) {
    throw new AppError(
      "VALIDATION_FAILED",
      "Cultivable area cannot exceed total area."
    );
  }

  return {
    cultivableAreaAcres,
    irrigationSource: cleanOptional(input.irrigationSource),
    ownershipType: cleanOptional(input.ownershipType),
    status: input.status ?? "ACTIVE",
    surveyNumber: cleanOptional(input.surveyNumber),
    totalAreaAcres
  };
}

function toPlotRequest(input: FarmPlotInput): CreateFarmPlotRequest {
  const latitude = parseOptionalNumber(input.latitude, "Latitude");
  const longitude = parseOptionalNumber(input.longitude, "Longitude");

  if (latitude !== undefined && (latitude < -90 || latitude > 90)) {
    throw new AppError("VALIDATION_FAILED", "Latitude must be between -90 and 90.");
  }

  if (longitude !== undefined && (longitude < -180 || longitude > 180)) {
    throw new AppError(
      "VALIDATION_FAILED",
      "Longitude must be between -180 and 180."
    );
  }

  return {
    areaAcres: parsePositiveNumber(input.areaAcres, "Plot area"),
    landholdingId: cleanOptional(input.landholdingId),
    latitude,
    longitude,
    plotName: input.plotName.trim(),
    soilType: cleanOptional(input.soilType),
    status: input.status ?? "ACTIVE"
  };
}

async function getLocalFarmLandholdings(memberId: string) {
  const saved = await readJsonArray<Partial<FarmLandholding>>([
    storageKeys.fpo.landholdings
  ]);

  return saved
    .map(toStoredFarmLandholding)
    .filter((item): item is FarmLandholding => Boolean(item))
    .filter((item) => item.memberId === memberId);
}

async function replaceLocalFarmLandholdings(
  memberId: string,
  landholdings: FarmLandholding[]
) {
  const saved = await readJsonArray<Partial<FarmLandholding>>([
    storageKeys.fpo.landholdings
  ]);
  const otherLandholdings = saved
    .map(toStoredFarmLandholding)
    .filter((item): item is FarmLandholding => Boolean(item))
    .filter((item) => item.memberId !== memberId);

  await writeJson(storageKeys.fpo.landholdings, [
    ...landholdings,
    ...otherLandholdings
  ]);
}

async function upsertLocalFarmLandholding(landholding: FarmLandholding) {
  const saved = await readJsonArray<Partial<FarmLandholding>>([
    storageKeys.fpo.landholdings
  ]);
  const current = saved
    .map(toStoredFarmLandholding)
    .filter((item): item is FarmLandholding => Boolean(item));

  await writeJson(storageKeys.fpo.landholdings, [
    landholding,
    ...current.filter((item) => item.id !== landholding.id)
  ]);
  return landholding;
}

async function getLocalFarmPlots(memberId: string) {
  const saved = await readJsonArray<Partial<FarmPlot>>([storageKeys.fpo.plots]);

  return saved
    .map(toStoredFarmPlot)
    .filter((item): item is FarmPlot => Boolean(item))
    .filter((item) => item.memberId === memberId);
}

async function replaceLocalFarmPlots(memberId: string, plots: FarmPlot[]) {
  const saved = await readJsonArray<Partial<FarmPlot>>([storageKeys.fpo.plots]);
  const otherPlots = saved
    .map(toStoredFarmPlot)
    .filter((item): item is FarmPlot => Boolean(item))
    .filter((item) => item.memberId !== memberId);

  await writeJson(storageKeys.fpo.plots, [...plots, ...otherPlots]);
}

async function upsertLocalFarmPlot(plot: FarmPlot) {
  const saved = await readJsonArray<Partial<FarmPlot>>([storageKeys.fpo.plots]);
  const current = saved
    .map(toStoredFarmPlot)
    .filter((item): item is FarmPlot => Boolean(item));

  await writeJson(storageKeys.fpo.plots, [
    plot,
    ...current.filter((item) => item.id !== plot.id)
  ]);
  return plot;
}

function parsePositiveNumber(value: string, label: string) {
  const parsed = parseOptionalNumber(value, label);

  if (parsed === undefined || parsed <= 0) {
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

function toFarmAssetError(error: unknown) {
  if (error instanceof ApiClientError) {
    if (error.status === 400) {
      return new AppError("VALIDATION_FAILED", error.message);
    }

    if (error.status === 401 || error.status === 403) {
      return new AppError("ACCESS_DENIED", error.message);
    }

    return new AppError("API_REQUEST_FAILED", error.message);
  }

  return new AppError("API_REQUEST_FAILED", "Unable to manage farm records.");
}
