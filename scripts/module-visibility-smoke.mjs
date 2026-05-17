import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const Module = require("node:module");
const ts = require("typescript");

Module._extensions[".ts"] = function loadTypeScript(module, filename) {
  const source = readFileSync(filename, "utf8");
  const output = ts.transpileModule(source, {
    compilerOptions: {
      esModuleInterop: true,
      jsx: ts.JsxEmit.React,
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022
    },
    fileName: filename
  }).outputText;

  module._compile(output, filename);
};

const {
  moduleVisibilityRegistry,
  getVisibleAdminTabs,
  getVisibleFarmerTabs,
  canRolePerform
} = require("../src/auth/roleAccess.ts");

const coreModules = [
  "ACTIVITY_COMPLIANCE",
  "ADVISORY",
  "EVIDENCE_REVIEW",
  "REPORT_EXPORT"
];
const fpoModules = [
  "MEMBER_DATA",
  "LAND_RECORDS",
  "GEO_TAGGING",
  "CROP_PLANNING",
  "INPUT_DEMAND",
  ...coreModules
];
const carbonModules = ["SUSTAINABILITY", ...coreModules];

function tabIds(tabs) {
  return tabs.map((tab) => tab.tab);
}

assert.deepEqual(Object.keys(moduleVisibilityRegistry.adminTabs).sort(), [
  "advisories",
  "carbon",
  "cropPlanning",
  "inputDemand",
  "overview",
  "participants",
  "reports",
  "roles",
  "workflows"
]);
assert.equal(moduleVisibilityRegistry.adminTabs.carbon.scope, "carbon");
assert.equal(moduleVisibilityRegistry.adminTabs.participants.scope, "fpo");
assert.equal(moduleVisibilityRegistry.farmerTabs.carbon.scope, "carbon");
assert.equal(moduleVisibilityRegistry.farmerTabs.cycles.scope, "fpo");

const carbonAdminTabs = tabIds(
  getVisibleAdminTabs("admin", carbonModules, {
    enabledClientModules: ["carbon"]
  })
);
assert.ok(carbonAdminTabs.includes("carbon"));
assert.ok(!carbonAdminTabs.includes("participants"));
assert.ok(!carbonAdminTabs.includes("cropPlanning"));
assert.ok(!carbonAdminTabs.includes("inputDemand"));

const carbonCoordinatorTabs = tabIds(
  getVisibleAdminTabs("fieldCoordinator", carbonModules, {
    enabledClientModules: ["carbon"]
  })
);
assert.ok(carbonCoordinatorTabs.includes("carbon"));
assert.ok(!carbonCoordinatorTabs.includes("roles"));

const carbonWithoutSubscriptionTabs = tabIds(
  getVisibleAdminTabs("admin", coreModules, {
    enabledClientModules: ["carbon"]
  })
);
assert.ok(!carbonWithoutSubscriptionTabs.includes("carbon"));

const fpoAdminTabs = tabIds(
  getVisibleAdminTabs("admin", fpoModules, {
    enabledClientModules: ["fpo"]
  })
);
assert.ok(!fpoAdminTabs.includes("carbon"));
assert.ok(fpoAdminTabs.includes("participants"));
assert.ok(fpoAdminTabs.includes("cropPlanning"));
assert.ok(fpoAdminTabs.includes("inputDemand"));

const fullPackageTabs = tabIds(
  getVisibleAdminTabs("admin", [...fpoModules, "SUSTAINABILITY"], {
    enabledClientModules: ["carbon", "fpo"]
  })
);
assert.ok(fullPackageTabs.includes("carbon"));
assert.ok(fullPackageTabs.includes("participants"));

const farmerCarbonTabs = tabIds(
  getVisibleFarmerTabs(carbonModules, {
    enabledClientModules: ["carbon"]
  })
);
assert.deepEqual(farmerCarbonTabs, ["carbon"]);
assert.ok(farmerCarbonTabs.includes("carbon"));

const farmerFpoTabs = tabIds(
  getVisibleFarmerTabs(fpoModules, {
    enabledClientModules: ["fpo"]
  })
);
assert.deepEqual(farmerFpoTabs, ["cycles", "dashboard", "profile", "history"]);

const farmerFullPackageTabs = tabIds(
  getVisibleFarmerTabs([...fpoModules, "SUSTAINABILITY"], {
    enabledClientModules: ["carbon", "fpo"]
  })
);
assert.deepEqual(farmerFullPackageTabs, [
  "cycles",
  "dashboard",
  "profile",
  "history",
  "carbon"
]);

const farmerWithoutSubscriptionTabs = tabIds(
  getVisibleFarmerTabs(coreModules, {
    enabledClientModules: ["carbon"]
  })
);
assert.ok(!farmerWithoutSubscriptionTabs.includes("carbon"));

assert.equal(canRolePerform("fpoManager", "manageInputDemand"), true);
assert.equal(
  canRolePerform("fpoManager", "manageInputDemand", {
    enabledModules: carbonModules,
    features: { enabledClientModules: ["carbon"] }
  }),
  false
);

console.log("Module visibility smoke passed.");
