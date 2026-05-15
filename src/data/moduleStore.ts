import AsyncStorage from "@react-native-async-storage/async-storage";

import { apiClient } from "../core/api/client";
import { endpoints } from "../core/api/endpoints";
import { logger } from "../core/logging/logger";
import { storageKeys } from "../core/storage/storageKeys";

export type PlatformModuleCode =
  | "ACTIVITY_COMPLIANCE"
  | "ADVISORY"
  | "ANALYTICS"
  | "CROP_PLANNING"
  | "EVIDENCE_REVIEW"
  | "GEO_TAGGING"
  | "INPUT_DEMAND"
  | "INVENTORY"
  | "LAND_RECORDS"
  | "MEMBER_DATA"
  | "PROCUREMENT"
  | "REPORT_EXPORT"
  | "SUSTAINABILITY"
  | "TRACEABILITY";

type EnabledModulesResponse = {
  modules: PlatformModuleCode[];
};

const defaultEnabledModules: PlatformModuleCode[] = [
  "ACTIVITY_COMPLIANCE",
  "ADVISORY",
  "CROP_PLANNING",
  "EVIDENCE_REVIEW",
  "GEO_TAGGING",
  "INPUT_DEMAND",
  "LAND_RECORDS",
  "MEMBER_DATA",
  "REPORT_EXPORT",
  "SUSTAINABILITY"
];

export async function loadEnabledModules(accessToken?: string | null) {
  const token = accessToken ?? (await AsyncStorage.getItem(storageKeys.auth.accessToken));

  if (!token) {
    return defaultEnabledModules;
  }

  try {
    const response = await apiClient.get<EnabledModulesResponse>(
      endpoints.platform.enabledModules,
      { accessToken: token }
    );
    await saveEnabledModules(response.modules);
    return response.modules;
  } catch (error) {
    logger.warn("Unable to load enabled platform modules; using cached modules.", {
      message: error instanceof Error ? error.message : "Unknown error"
    });
    return getCachedEnabledModules();
  }
}

export async function getCachedEnabledModules() {
  const saved = await AsyncStorage.getItem(storageKeys.platform.enabledModules);

  if (!saved) {
    return defaultEnabledModules;
  }

  try {
    const parsed = JSON.parse(saved);
    if (!Array.isArray(parsed)) {
      return defaultEnabledModules;
    }

    return parsed.filter(isPlatformModuleCode);
  } catch {
    return defaultEnabledModules;
  }
}

export async function saveEnabledModules(modules: PlatformModuleCode[]) {
  await AsyncStorage.setItem(
    storageKeys.platform.enabledModules,
    JSON.stringify(modules.filter(isPlatformModuleCode))
  );
}

export function isModuleEnabled(
  modules: PlatformModuleCode[],
  moduleCode: PlatformModuleCode
) {
  return modules.includes(moduleCode);
}

function isPlatformModuleCode(value: unknown): value is PlatformModuleCode {
  return typeof value === "string" && defaultAllModuleCodes.includes(value as PlatformModuleCode);
}

const defaultAllModuleCodes: PlatformModuleCode[] = [
  "ACTIVITY_COMPLIANCE",
  "ADVISORY",
  "ANALYTICS",
  "CROP_PLANNING",
  "EVIDENCE_REVIEW",
  "GEO_TAGGING",
  "INPUT_DEMAND",
  "INVENTORY",
  "LAND_RECORDS",
  "MEMBER_DATA",
  "PROCUREMENT",
  "REPORT_EXPORT",
  "SUSTAINABILITY",
  "TRACEABILITY"
];
