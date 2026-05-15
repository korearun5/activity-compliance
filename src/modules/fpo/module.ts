import type { ClientModuleDefinition } from "../types";

export const fpoModule: ClientModuleDefinition = {
  backendModules: [
    "MEMBER_DATA",
    "LAND_RECORDS",
    "GEO_TAGGING",
    "CROP_PLANNING",
    "INPUT_DEMAND",
    "REPORT_EXPORT"
  ],
  description:
    "FPO operations for farmer profiles, land records, crop planning, input demand, and FPO Excel reports.",
  id: "fpo",
  label: "FPO Operations"
};
