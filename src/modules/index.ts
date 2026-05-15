import { appConfig } from "../core/config/appConfig";
import { carbonModule } from "./carbon";
import { fpoModule } from "./fpo/module";
import type { ClientModuleDefinition, ClientModuleId } from "./types";

export const clientModules: ClientModuleDefinition[] = [carbonModule, fpoModule];

const enabledClientModuleIds = new Set(
  appConfig.enabledClientModules.filter(isClientModuleId)
);

export function isClientModuleEnabled(moduleId: ClientModuleId) {
  return enabledClientModuleIds.has(moduleId);
}

export function getEnabledClientModules() {
  return clientModules.filter((module) => isClientModuleEnabled(module.id));
}

export function getEnabledClientModuleIds() {
  return clientModules
    .map((module) => module.id)
    .filter(isClientModuleEnabled);
}

function isClientModuleId(value: string): value is ClientModuleId {
  return clientModules.some((module) => module.id === value);
}
