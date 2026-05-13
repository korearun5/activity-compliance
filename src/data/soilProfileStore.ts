import AsyncStorage from "@react-native-async-storage/async-storage";

import { apiClient, ApiClientError } from "../core/api/client";
import { endpoints } from "../core/api/endpoints";
import {
  FpoSoilProfileRequest,
  FpoSoilProfileResponse
} from "../core/api/fpoContracts";
import { AppError } from "../core/errors/AppError";
import { logger } from "../core/logging/logger";
import { readJsonArray, writeJson } from "../core/storage/jsonStore";
import { storageKeys } from "../core/storage/storageKeys";

export type SoilProfile = {
  createdAt: string;
  id: string;
  memberId: string;
  memberNumber: string;
  nitrogen?: number;
  notes?: string;
  ph?: number;
  phosphorus?: number;
  potassium?: number;
  reportContentType?: string;
  reportFileName?: string;
  reportUrl?: string;
  soilOrganicCarbon?: number;
  tenantId?: string;
  updatedAt: string;
};

export type SoilProfileInput = {
  nitrogen?: string;
  notes?: string;
  ph?: string;
  phosphorus?: string;
  potassium?: string;
  reportContentType?: string;
  reportFileName?: string;
  reportUrl?: string;
  soilOrganicCarbon?: string;
};

export async function getSoilProfiles(memberId: string): Promise<SoilProfile[]> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.get<FpoSoilProfileResponse[]>(
        endpoints.fpo.soilProfiles.listByMember(memberId),
        { accessToken }
      );
      const profiles = response.map(toSoilProfile);

      await replaceLocalSoilProfiles(memberId, profiles);
      return profiles;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toSoilProfileError(error);
      }

      logger.warn("Backend soil profile list unavailable; using cached records.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return getLocalSoilProfiles(memberId);
}

export async function createSoilProfile(
  memberId: string,
  memberNumber: string,
  input: SoilProfileInput
): Promise<SoilProfile> {
  const request = toSoilProfileRequest(input);
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.post<
        FpoSoilProfileRequest,
        FpoSoilProfileResponse
      >(endpoints.fpo.soilProfiles.createForMember(memberId), request, {
        accessToken
      });
      const profile = toSoilProfile(response);

      await upsertLocalSoilProfile(profile);
      return profile;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toSoilProfileError(error);
      }

      logger.warn("Backend soil profile creation unavailable; using cached records.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return upsertLocalSoilProfile({
    ...request,
    createdAt: new Date().toISOString(),
    id: `local-soil-profile-${Date.now()}`,
    memberId,
    memberNumber,
    updatedAt: new Date().toISOString()
  });
}

function toSoilProfile(response: FpoSoilProfileResponse): SoilProfile {
  return {
    createdAt: response.createdAt,
    id: response.id,
    memberId: response.memberId,
    memberNumber: response.memberNumber,
    nitrogen: response.nitrogen ?? undefined,
    notes: response.notes ?? undefined,
    ph: response.ph ?? undefined,
    phosphorus: response.phosphorus ?? undefined,
    potassium: response.potassium ?? undefined,
    reportContentType: response.reportContentType ?? undefined,
    reportFileName: response.reportFileName ?? undefined,
    reportUrl: response.reportUrl ?? undefined,
    soilOrganicCarbon: response.soilOrganicCarbon ?? undefined,
    tenantId: response.tenantId,
    updatedAt: response.updatedAt
  };
}

function toStoredSoilProfile(profile: Partial<SoilProfile>): SoilProfile | null {
  if (
    typeof profile.id !== "string" ||
    typeof profile.memberId !== "string" ||
    typeof profile.memberNumber !== "string" ||
    typeof profile.createdAt !== "string" ||
    typeof profile.updatedAt !== "string"
  ) {
    return null;
  }

  return {
    createdAt: profile.createdAt,
    id: profile.id,
    memberId: profile.memberId,
    memberNumber: profile.memberNumber,
    nitrogen: profile.nitrogen,
    notes: profile.notes,
    ph: profile.ph,
    phosphorus: profile.phosphorus,
    potassium: profile.potassium,
    reportContentType: profile.reportContentType,
    reportFileName: profile.reportFileName,
    reportUrl: profile.reportUrl,
    soilOrganicCarbon: profile.soilOrganicCarbon,
    tenantId: profile.tenantId,
    updatedAt: profile.updatedAt
  };
}

function toSoilProfileRequest(input: SoilProfileInput): FpoSoilProfileRequest {
  return {
    nitrogen: parseOptionalNonNegativeNumber(input.nitrogen, "Nitrogen"),
    notes: cleanOptional(input.notes),
    ph: parseOptionalPh(input.ph),
    phosphorus: parseOptionalNonNegativeNumber(input.phosphorus, "Phosphorus"),
    potassium: parseOptionalNonNegativeNumber(input.potassium, "Potassium"),
    reportContentType: cleanOptional(input.reportContentType),
    reportFileName: cleanOptional(input.reportFileName),
    reportUrl: parseOptionalUrl(input.reportUrl),
    soilOrganicCarbon: parseOptionalNonNegativeNumber(
      input.soilOrganicCarbon,
      "SOC"
    )
  };
}

async function getLocalSoilProfiles(memberId: string) {
  const saved = await readJsonArray<Partial<SoilProfile>>([
    storageKeys.fpo.soilProfiles
  ]);

  return saved
    .map(toStoredSoilProfile)
    .filter((item): item is SoilProfile => Boolean(item))
    .filter((item) => item.memberId === memberId);
}

async function replaceLocalSoilProfiles(memberId: string, profiles: SoilProfile[]) {
  const saved = await readJsonArray<Partial<SoilProfile>>([
    storageKeys.fpo.soilProfiles
  ]);
  const otherProfiles = saved
    .map(toStoredSoilProfile)
    .filter((item): item is SoilProfile => Boolean(item))
    .filter((item) => item.memberId !== memberId);

  await writeJson(storageKeys.fpo.soilProfiles, [...profiles, ...otherProfiles]);
}

async function upsertLocalSoilProfile(profile: SoilProfile) {
  const saved = await readJsonArray<Partial<SoilProfile>>([
    storageKeys.fpo.soilProfiles
  ]);
  const current = saved
    .map(toStoredSoilProfile)
    .filter((item): item is SoilProfile => Boolean(item));

  await writeJson(storageKeys.fpo.soilProfiles, [
    profile,
    ...current.filter((item) => item.id !== profile.id)
  ]);
  return profile;
}

function parseOptionalPh(value: string | undefined) {
  const parsed = parseOptionalNonNegativeNumber(value, "pH");

  if (parsed !== undefined && parsed > 14) {
    throw new AppError("VALIDATION_FAILED", "pH cannot be greater than 14.");
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

function parseOptionalUrl(value: string | undefined) {
  const trimmed = cleanOptional(value);

  if (!trimmed) {
    return undefined;
  }

  if (!/^https?:\/\//i.test(trimmed)) {
    throw new AppError(
      "VALIDATION_FAILED",
      "Soil report URL must start with http or https."
    );
  }

  return trimmed;
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

function toSoilProfileError(error: unknown) {
  if (error instanceof ApiClientError) {
    if (error.status === 400) {
      return new AppError("VALIDATION_FAILED", error.message);
    }

    if (error.status === 401 || error.status === 403) {
      return new AppError("ACCESS_DENIED", error.message);
    }

    return new AppError("API_REQUEST_FAILED", error.message);
  }

  return new AppError("API_REQUEST_FAILED", "Unable to manage soil profiles.");
}
