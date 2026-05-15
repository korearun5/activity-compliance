import type { PlatformModuleCode } from "../data/moduleStore";
import type { ClientModuleId } from "../modules/types";
import type { UserRole } from "../core/model/types";

export type StaffRole = Exclude<UserRole, "farmer">;

export type UiFeatureFlags = {
  enabledClientModules: readonly ClientModuleId[];
};

export type AdminTabId =
  | "advisories"
  | "carbon"
  | "cropPlanning"
  | "inputDemand"
  | "overview"
  | "participants"
  | "reports"
  | "roles"
  | "workflows";

export type FarmerTabId = "cycles" | "dashboard" | "profile" | "history" | "carbon";

export type RoleAction =
  | "createFieldCoordinator"
  | "createFpoManager"
  | "exportComplianceReport"
  | "manageAdvisories"
  | "manageCropMasterData"
  | "manageInputDemand"
  | "manageStaffRoles"
  | "manageWorkflowDefinitions"
  | "reviewEvidence"
  | "viewReportSummary";

type TabAccess<TTab extends string> = {
  clientModule?: ClientModuleId;
  label: string;
  module?: PlatformModuleCode;
  roles?: StaffRole[];
  tab: TTab;
};

const adminAndFpoRoles: StaffRole[] = ["admin", "fpoManager"];
const allStaffRoles: StaffRole[] = ["admin", "fpoManager", "fieldCoordinator"];

export const adminTabAccess: Record<AdminTabId, TabAccess<AdminTabId>> = {
  advisories: {
    label: "Advisories",
    module: "ADVISORY",
    roles: allStaffRoles,
    tab: "advisories"
  },
  carbon: {
    clientModule: "carbon",
    label: "Carbon",
    module: "SUSTAINABILITY",
    roles: adminAndFpoRoles,
    tab: "carbon"
  },
  cropPlanning: {
    clientModule: "fpo",
    label: "Crop Planning",
    module: "CROP_PLANNING",
    roles: allStaffRoles,
    tab: "cropPlanning"
  },
  inputDemand: {
    clientModule: "fpo",
    label: "Input Demand",
    module: "INPUT_DEMAND",
    roles: adminAndFpoRoles,
    tab: "inputDemand"
  },
  overview: {
    label: "Overview",
    roles: allStaffRoles,
    tab: "overview"
  },
  participants: {
    clientModule: "fpo",
    label: "Farmers",
    module: "MEMBER_DATA",
    roles: allStaffRoles,
    tab: "participants"
  },
  reports: {
    label: "Reports",
    module: "REPORT_EXPORT",
    roles: allStaffRoles,
    tab: "reports"
  },
  roles: {
    label: "Roles",
    roles: adminAndFpoRoles,
    tab: "roles"
  },
  workflows: {
    label: "Workflows",
    module: "ACTIVITY_COMPLIANCE",
    roles: adminAndFpoRoles,
    tab: "workflows"
  }
};

export const adminTabOrder: AdminTabId[] = [
  "overview",
  "workflows",
  "participants",
  "cropPlanning",
  "inputDemand",
  "roles",
  "advisories",
  "reports",
  "carbon"
];

export const farmerTabAccess: Record<FarmerTabId, TabAccess<FarmerTabId>> = {
  carbon: {
    clientModule: "carbon",
    label: "Carbon",
    module: "SUSTAINABILITY",
    tab: "carbon"
  },
  cycles: {
    label: "Cycles",
    tab: "cycles"
  },
  dashboard: {
    label: "Dashboard",
    tab: "dashboard"
  },
  history: {
    label: "History",
    tab: "history"
  },
  profile: {
    label: "Profile",
    tab: "profile"
  }
};

export const farmerTabOrder: FarmerTabId[] = [
  "cycles",
  "dashboard",
  "profile",
  "history",
  "carbon"
];

const actionAccess: Record<RoleAction, UserRole[]> = {
  createFieldCoordinator: ["admin", "fpoManager"],
  createFpoManager: ["admin"],
  exportComplianceReport: ["admin", "fpoManager"],
  manageAdvisories: ["admin", "fpoManager"],
  manageCropMasterData: ["admin", "fpoManager"],
  manageInputDemand: ["admin", "fpoManager"],
  manageStaffRoles: ["admin"],
  manageWorkflowDefinitions: ["admin", "fpoManager"],
  reviewEvidence: ["admin", "fpoManager"],
  viewReportSummary: ["admin", "fpoManager"]
};

export function canRolePerform(role: UserRole, action: RoleAction) {
  return actionAccess[action].includes(role);
}

export function getVisibleAdminTabs(
  role: StaffRole,
  enabledModules: PlatformModuleCode[] | null,
  features: UiFeatureFlags
) {
  return adminTabOrder
    .map((tab) => adminTabAccess[tab])
    .filter(
      (item) =>
        (!item.roles || item.roles.includes(role)) &&
        isTabFeatureAvailable(item, features) &&
        isTabModuleAvailable(item.module, enabledModules)
    );
}

export function getVisibleFarmerTabs(
  enabledModules: PlatformModuleCode[] | null,
  features: UiFeatureFlags
) {
  return farmerTabOrder
    .map((tab) => farmerTabAccess[tab])
    .filter(
      (item) =>
        isTabFeatureAvailable(item, features) &&
        isTabModuleAvailable(item.module, enabledModules)
    );
}

function isTabFeatureAvailable<TTab extends string>(
  item: TabAccess<TTab>,
  features: UiFeatureFlags
) {
  return (
    !item.clientModule || features.enabledClientModules.includes(item.clientModule)
  );
}

function isTabModuleAvailable(
  moduleCode: PlatformModuleCode | undefined,
  enabledModules: PlatformModuleCode[] | null
) {
  return !moduleCode || enabledModules === null || enabledModules.includes(moduleCode);
}
