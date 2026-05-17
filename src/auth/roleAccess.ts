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

export type VisibilityScope = "common" | ClientModuleId;

export type VisibilityContext = {
  enabledModules: PlatformModuleCode[] | null;
  features: UiFeatureFlags;
};

type VisibilityRule<TId extends string, TRole extends UserRole> = {
  id: TId;
  clientModule?: ClientModuleId;
  kind: "action" | "adminTab" | "farmerTab";
  label: string;
  module?: PlatformModuleCode;
  roles?: readonly TRole[];
  scope: VisibilityScope;
};

type TabAccess<TTab extends string> = VisibilityRule<TTab, UserRole> & {
  tab: TTab;
};

type AdminTabAccess = TabAccess<AdminTabId> & {
  kind: "adminTab";
  roles: readonly StaffRole[];
};

type FarmerTabAccess = TabAccess<FarmerTabId> & {
  kind: "farmerTab";
};

type ActionAccess = VisibilityRule<RoleAction, UserRole> & {
  kind: "action";
};

const adminAndFpoRoles = ["admin", "fpoManager"] as const;
const allStaffRoles = ["admin", "fpoManager", "fieldCoordinator"] as const;
const allUserRoles = ["admin", "fpoManager", "fieldCoordinator", "farmer"] as const;

export const moduleVisibilityRegistry = {
  actions: {
    createFieldCoordinator: {
      id: "createFieldCoordinator",
      kind: "action",
      label: "Create field coordinator",
      roles: adminAndFpoRoles,
      scope: "common"
    },
    createFpoManager: {
      id: "createFpoManager",
      kind: "action",
      label: "Create FPO manager",
      roles: ["admin"],
      scope: "common"
    },
    exportComplianceReport: {
      id: "exportComplianceReport",
      kind: "action",
      label: "Export compliance report",
      module: "REPORT_EXPORT",
      roles: adminAndFpoRoles,
      scope: "common"
    },
    manageAdvisories: {
      id: "manageAdvisories",
      kind: "action",
      label: "Manage advisories",
      module: "ADVISORY",
      roles: adminAndFpoRoles,
      scope: "common"
    },
    manageCropMasterData: {
      clientModule: "fpo",
      id: "manageCropMasterData",
      kind: "action",
      label: "Manage crop master data",
      module: "CROP_PLANNING",
      roles: adminAndFpoRoles,
      scope: "fpo"
    },
    manageInputDemand: {
      clientModule: "fpo",
      id: "manageInputDemand",
      kind: "action",
      label: "Manage input demand",
      module: "INPUT_DEMAND",
      roles: adminAndFpoRoles,
      scope: "fpo"
    },
    manageStaffRoles: {
      id: "manageStaffRoles",
      kind: "action",
      label: "Manage staff roles",
      roles: ["admin"],
      scope: "common"
    },
    manageWorkflowDefinitions: {
      id: "manageWorkflowDefinitions",
      kind: "action",
      label: "Manage workflow definitions",
      module: "ACTIVITY_COMPLIANCE",
      roles: adminAndFpoRoles,
      scope: "common"
    },
    reviewEvidence: {
      id: "reviewEvidence",
      kind: "action",
      label: "Review evidence",
      module: "EVIDENCE_REVIEW",
      roles: adminAndFpoRoles,
      scope: "common"
    },
    viewReportSummary: {
      id: "viewReportSummary",
      kind: "action",
      label: "View report summary",
      module: "REPORT_EXPORT",
      roles: adminAndFpoRoles,
      scope: "common"
    }
  } satisfies Record<RoleAction, ActionAccess>,
  adminTabs: {
    advisories: {
      id: "advisories",
      kind: "adminTab",
      label: "Advisories",
      module: "ADVISORY",
      roles: allStaffRoles,
      scope: "common",
      tab: "advisories"
    },
    carbon: {
      clientModule: "carbon",
      id: "carbon",
      kind: "adminTab",
      label: "Carbon",
      module: "SUSTAINABILITY",
      roles: allStaffRoles,
      scope: "carbon",
      tab: "carbon"
    },
    cropPlanning: {
      clientModule: "fpo",
      id: "cropPlanning",
      kind: "adminTab",
      label: "Crop Planning",
      module: "CROP_PLANNING",
      roles: allStaffRoles,
      scope: "fpo",
      tab: "cropPlanning"
    },
    inputDemand: {
      clientModule: "fpo",
      id: "inputDemand",
      kind: "adminTab",
      label: "Input Demand",
      module: "INPUT_DEMAND",
      roles: adminAndFpoRoles,
      scope: "fpo",
      tab: "inputDemand"
    },
    overview: {
      id: "overview",
      kind: "adminTab",
      label: "Overview",
      roles: allStaffRoles,
      scope: "common",
      tab: "overview"
    },
    participants: {
      clientModule: "fpo",
      id: "participants",
      kind: "adminTab",
      label: "Farmers",
      module: "MEMBER_DATA",
      roles: allStaffRoles,
      scope: "fpo",
      tab: "participants"
    },
    reports: {
      id: "reports",
      kind: "adminTab",
      label: "Reports",
      module: "REPORT_EXPORT",
      roles: allStaffRoles,
      scope: "common",
      tab: "reports"
    },
    roles: {
      id: "roles",
      kind: "adminTab",
      label: "Roles",
      roles: adminAndFpoRoles,
      scope: "common",
      tab: "roles"
    },
    workflows: {
      id: "workflows",
      kind: "adminTab",
      label: "Workflows",
      module: "ACTIVITY_COMPLIANCE",
      roles: adminAndFpoRoles,
      scope: "common",
      tab: "workflows"
    }
  } satisfies Record<AdminTabId, AdminTabAccess>,
  farmerTabs: {
    carbon: {
      clientModule: "carbon",
      id: "carbon",
      kind: "farmerTab",
      label: "Carbon",
      module: "SUSTAINABILITY",
      scope: "carbon",
      tab: "carbon"
    },
    cycles: {
      clientModule: "fpo",
      id: "cycles",
      kind: "farmerTab",
      label: "Cycles",
      module: "ACTIVITY_COMPLIANCE",
      scope: "fpo",
      tab: "cycles"
    },
    dashboard: {
      clientModule: "fpo",
      id: "dashboard",
      kind: "farmerTab",
      label: "Dashboard",
      module: "ACTIVITY_COMPLIANCE",
      scope: "fpo",
      tab: "dashboard"
    },
    history: {
      clientModule: "fpo",
      id: "history",
      kind: "farmerTab",
      label: "History",
      module: "ACTIVITY_COMPLIANCE",
      scope: "fpo",
      tab: "history"
    },
    profile: {
      clientModule: "fpo",
      id: "profile",
      kind: "farmerTab",
      label: "Profile",
      module: "MEMBER_DATA",
      scope: "fpo",
      tab: "profile"
    }
  } satisfies Record<FarmerTabId, FarmerTabAccess>
} as const;

export const adminTabAccess = moduleVisibilityRegistry.adminTabs;

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

export const farmerTabAccess = moduleVisibilityRegistry.farmerTabs;

export const farmerTabOrder: FarmerTabId[] = [
  "cycles",
  "dashboard",
  "profile",
  "history",
  "carbon"
];

export function canRolePerform(
  role: UserRole,
  action: RoleAction,
  context?: VisibilityContext
) {
  const rule = moduleVisibilityRegistry.actions[action];
  return isRuleVisibleForRole(rule, role, context);
}

export function getVisibleAdminTabs(
  role: StaffRole,
  enabledModules: PlatformModuleCode[] | null,
  features: UiFeatureFlags
) {
  return adminTabOrder
    .map((tab) => adminTabAccess[tab])
    .filter((item) => isRuleVisibleForRole(item, role, { enabledModules, features }));
}

export function getVisibleFarmerTabs(
  enabledModules: PlatformModuleCode[] | null,
  features: UiFeatureFlags
) {
  return farmerTabOrder
    .map((tab) => farmerTabAccess[tab])
    .filter((item) =>
      isRuleVisibleForRole(item, "farmer", { enabledModules, features })
    );
}

function isRuleVisibleForRole<TId extends string, TRole extends UserRole>(
  item: VisibilityRule<TId, TRole>,
  role: UserRole,
  context?: VisibilityContext
) {
  const roles = item.roles ?? allUserRoles;
  const roleAllowed = roles.includes(role as TRole);

  if (!roleAllowed) {
    return false;
  }

  if (!context) {
    return true;
  }

  return (
    isClientModuleAvailable(getRuleClientModule(item), context.features) &&
    isBackendModuleAvailable(item.module, context.enabledModules)
  );
}

function getRuleClientModule<TId extends string, TRole extends UserRole>(
  item: VisibilityRule<TId, TRole>
): ClientModuleId | undefined {
  return item.scope === "common" ? item.clientModule : item.scope;
}

function isClientModuleAvailable(
  clientModule: ClientModuleId | undefined,
  features: UiFeatureFlags
) {
  return !clientModule || features.enabledClientModules.includes(clientModule);
}

function isBackendModuleAvailable(
  moduleCode: PlatformModuleCode | undefined,
  enabledModules: PlatformModuleCode[] | null
) {
  return !moduleCode || enabledModules === null || enabledModules.includes(moduleCode);
}
