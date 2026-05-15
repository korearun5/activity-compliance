import type { PlatformModuleCode } from "../data/moduleStore";

export type ClientModuleId = "carbon" | "fpo";

export type ClientModuleDefinition = {
  backendModules: PlatformModuleCode[];
  description: string;
  id: ClientModuleId;
  label: string;
};
