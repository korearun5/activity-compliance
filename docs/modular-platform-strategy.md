# Modular Platform Strategy

This document records an important product and architecture decision:

```text
Build a modular monolith first. Do not start with microservices.
```

The platform has multiple possible service modules: member data, crop planning,
input demand, advisory, activity compliance, inventory, procurement,
traceability, sustainability, analytics, and more. Clients may buy only one
module or a small package of modules. The codebase and delivery model must
support that without forcing us to give away all future modules.

## Why Not Microservices Now

Microservices are not the right starting point for this project.

Reasons:

- Product scope is still evolving.
- Phase 1 needs fast iteration.
- One team will likely build and support the first release.
- Cross-module workflows need shared transactions and simple debugging.
- Microservices increase DevOps, deployment, logging, monitoring, testing, and
  network failure complexity.
- Splitting too early creates artificial boundaries before the domain is stable.

Use a modular monolith now. Split only the modules that become operationally
heavy later.

## Target Architecture Now

Backend:

```text
backend/src/main/java/com/activityplatform/backend/
  auth/
  tenant/
  module/
  user/
  fpo/
    member/
    land/
    crop/
    inputdemand/
    advisory/
  activity/
  evidence/
  reporting/
  notification/
  inventory/        future
  procurement/      future
  traceability/     future
  sustainability/   future
```

Frontend:

```text
src/
  auth/
  core/
  modules/
    fpo-member/
    land-records/
    crop-planning/
    input-demand/
    advisory/
    activity-compliance/
    reports/
    inventory/        future
    procurement/      future
    traceability/     future
    sustainability/   future
```

Current frontend folders can evolve gradually. Do not block feature work on a
large folder refactor, but every new FPO module should be kept behind clear
store/API/screen boundaries.

## Module Packaging Principle

Every paid service should be represented as a platform module.

Suggested module codes:

| Module Code | Purpose |
| --- | --- |
| `MEMBER_DATA` | Farmer/member profile management. |
| `LAND_RECORDS` | Landholding records. |
| `GEO_TAGGING` | Plot coordinates and geo-tag reports. |
| `CROP_PLANNING` | Crop history and seasonal crop plans. |
| `INPUT_DEMAND` | Input rules, acreage computation, demand reports. |
| `ADVISORY` | Advisory templates and farmer/coordinator messages. |
| `ACTIVITY_COMPLIANCE` | Workflow/task timeline, proof upload, audit reports. |
| `EVIDENCE_REVIEW` | Admin/supervisor proof review. |
| `REPORT_EXPORT` | PDF/Excel export generation. |
| `INVENTORY` | Input inventory and stock movement. |
| `PROCUREMENT` | Produce aggregation/procurement records. |
| `TRACEABILITY` | Lot tracking, QR traceability, buyer visibility. |
| `SUSTAINABILITY` | Sustainable practice and carbon-related records. |
| `ANALYTICS` | Advanced dashboards, forecasting, data marts. |

## Tenant Module Subscription Model

Add module availability at tenant level.

Recommended tables:

```text
platform_modules
tenant_module_subscriptions
```

Suggested `platform_modules` columns:

- `id`
- `code`
- `name`
- `description`
- `status`
- `created_at`

Suggested `tenant_module_subscriptions` columns:

- `id`
- `tenant_id`
- `module_id`
- `status`
- `enabled_at`
- `disabled_at`
- `expires_at`
- `metadata`

Suggested statuses:

- `ENABLED`
- `DISABLED`
- `TRIAL`
- `EXPIRED`

Business rule:

- A user role controls what the user may do.
- A tenant module subscription controls what the tenant has purchased/enabled.
- Both checks are required.

## Backend Module Guard

Every paid module API must check module availability.

Preferred developer shape:

```java
@RequiresModule(ModuleCode.INPUT_DEMAND)
@GetMapping("/api/v1/fpo/demand-estimates")
public ApiResponse<?> listDemandEstimates() {
  ...
}
```

If annotation/aspect is too much for the first slice, use a direct service
guard:

```java
tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.INPUT_DEMAND);
```

Expected error:

```http
403 MODULE_NOT_ENABLED
```

The response should use the standard API envelope.

## Frontend Module Guard

Frontend must hide disabled modules after login.

Flow:

1. User logs in.
2. Frontend loads enabled module codes for the tenant/user.
3. Navigation tabs are built from enabled modules.
4. Store actions should still handle backend `MODULE_NOT_ENABLED` errors.

Example:

```ts
if (enabledModules.includes("INPUT_DEMAND")) {
  tabs.push("Input Demand");
}
```

Do not rely on frontend hiding alone. Backend must always enforce module
availability.

## Delivery Models

### Model A: Managed SaaS / Hosted By Us

Best model for modular pricing.

Client gets:

- Login access.
- Purchased modules.
- Support and updates.

Client does not get:

- Source code.
- Ability to unlock disabled modules.

Module control:

- Tenant module subscriptions managed by BaseCraft.

### Model B: Private Deployment Without Source Code

Good for larger or enterprise clients.

Client gets:

- Deployment on their cloud/server.
- Docker images or build artifacts.
- Admin access.

Client does not get:

- Source code by default.
- Future modules not purchased.

Module control:

- License key, subscription config, or admin-controlled tenant module table.

### Model C: Source Code Handover

Highest commercial risk.

If source code handover is required:

- Price must be much higher.
- Deliver only purchased modules.
- Remove unrelated controllers/screens/services from delivery branch.
- Do not ship future modules merely disabled by config.
- Contract must define source license and module ownership clearly.

Rule:

```text
Do not hand over full platform source code for a single-module price.
```

## Coding Implications

Do:

- Keep module boundaries clean.
- Add one backend package per product module.
- Add one frontend module/store/screen set per product module.
- Add tenant module checks to backend APIs.
- Hide disabled modules in frontend navigation.
- Keep shared utilities in `core`/`common`.
- Add tests for module-enabled and module-disabled cases.

Avoid:

- Large hardcoded if/else blocks scattered across screens.
- Shipping future modules in source handover builds.
- Using frontend-only hiding as licensing control.
- Splitting into microservices before a module has real operational pressure.

## When To Split Into Microservices Later

Only split a module when there is a clear operational reason.

Good candidates:

| Future Service | Split When |
| --- | --- |
| Notification service | SMS/WhatsApp/email volume grows or delivery retries need workers. |
| Report/export service | PDF/Excel jobs become slow or need async workers. |
| Geospatial/satellite service | NDVI/GIS processing becomes compute-heavy. |
| IoT ingestion service | Sensor readings become frequent/time-series heavy. |
| Analytics service | Dashboards need a warehouse, OLAP, or heavy aggregation. |
| Integration service | Many external APIs need isolated retry/rate-limit handling. |

Until then, keep the system as a modular monolith.

## Developer Tasks For Module Foundation

| ID | Status | Task | Notes |
| --- | --- | --- | --- |
| MOD-001 | Done | Add module code enum | Includes Phase 1 and future module codes. |
| MOD-002 | Done | Add `platform_modules` migration | Seeds known modules through Flyway. |
| MOD-003 | Done | Add `tenant_module_subscriptions` migration | Tenant/module/status relationship. |
| MOD-004 | Done | Add tenant module repository/service | Queries enabled modules by tenant. |
| MOD-005 | Done | Add backend module guard | Explicit `TenantModuleService.requireEnabled` check. |
| MOD-006 | Done | Add `MODULE_NOT_ENABLED` error code | Uses standard API envelope. |
| MOD-007 | Done | Add endpoint to list enabled modules | `GET /api/v1/platform/modules/enabled`. |
| MOD-008 | Done | Add admin-only module management API | `GET/PUT /api/v1/platform/module-subscriptions`. |
| MOD-009 | Done | Add frontend enabled-module store | Loads after login and caches with session. |
| MOD-010 | Done | Guard frontend navigation tabs | Hides disabled admin tabs. |
| MOD-011 | Done | Add tests for disabled module access | Targeted integration tests added; require Docker/Testcontainers to run. |
| MOD-012 | Pending | Document source-handover packaging process | Required before any source delivery. |

## Implemented Module API Surface

- `GET /api/v1/platform/modules`: public module catalog.
- `GET /api/v1/platform/modules/enabled`: authenticated list of enabled module
  codes for the current tenant.
- `GET /api/v1/platform/module-subscriptions`: admin-only current-tenant
  subscription list.
- `PUT /api/v1/platform/module-subscriptions/{moduleCode}`: admin-only enable
  or disable a current-tenant module.

Backend APIs that belong to a sellable module should call
`TenantModuleService.requireEnabled(currentTenantId, ModuleCode.X)` before
performing business work. The frontend should use the enabled-module store only
for navigation and UX; backend guards remain the licensing boundary.

## Phase 1 Module Defaults

For an FPO Phase 1 MVP, enable:

- `MEMBER_DATA`
- `LAND_RECORDS`
- `GEO_TAGGING`
- `CROP_PLANNING`
- `INPUT_DEMAND`
- `ADVISORY` as basic in-app advisory only
- `REPORT_EXPORT`

Optional if the client wants proof-based field workflow:

- `ACTIVITY_COMPLIANCE`
- `EVIDENCE_REVIEW`

Keep disabled unless separately sold:

- `INVENTORY`
- `PROCUREMENT`
- `TRACEABILITY`
- `SUSTAINABILITY`
- `ANALYTICS`
