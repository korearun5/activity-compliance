import type { ClientModuleDefinition } from "../types";

export const carbonModule: ClientModuleDefinition = {
  backendModules: ["SUSTAINABILITY", "ADVISORY", "ACTIVITY_COMPLIANCE"],
  description:
    "Carbon accounting, soil health scoring, carbon activities, advisories, dealer discovery, and carbon reporting.",
  id: "carbon",
  label: "Carbon Accounting"
};
