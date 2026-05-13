# FPO Developer Task List

This is the execution checklist for developers. Use this file when picking the
next task. Use [FPO MVP Roadmap](fpo-mvp-roadmap.md) for the business context,
phase strategy, and commercial assumptions.
Use [Modular Platform Strategy](modular-platform-strategy.md) before building
module guards, packaging rules, or future service boundaries.
Use [Project Status And Gap Register](project-status-and-gap-register.md) for
live completion percentages and go-live gaps.

## Task Status Legend

| Status  | Meaning                                                   |
| ------- | --------------------------------------------------------- |
| Done    | Already implemented in the current generic platform.      |
| Partial | Foundation exists, but FPO-specific work is still needed. |
| Pending | Ready to plan/build after scope confirmation.             |
| Blocked | Needs client/provider/commercial decision first.          |
| Future  | Not part of Phase 1 MVP.                                  |

## Current Foundation Tasks

These are already implemented and should be reused instead of rebuilt.

| ID       | Status | Area                 | What Exists                                   | Developer Note                                                       |
| -------- | ------ | -------------------- | --------------------------------------------- | -------------------------------------------------------------------- |
| CORE-001 | Done   | Backend foundation   | Spring Boot, PostgreSQL, Flyway, JPA          | Add new FPO schema via Flyway only.                                  |
| CORE-002 | Done   | Auth                 | JWT login, refresh, current user              | OTP can be added later without replacing JWT.                        |
| CORE-003 | Partial | Roles                | JWT role infrastructure and role APIs          | Align code to Phase 1 roles `ADMIN`, `FPO_MANAGER`, `FIELD_COORDINATOR`. |
| CORE-004 | Done   | User management      | Admin/supervisor can create participant users | Extend with FPO member profile table.                                |
| CORE-005 | Done   | Workflow definitions | Configurable workflow/task templates          | Reuse for crop activity schedules.                                   |
| CORE-006 | Done   | Activities           | Activity start and task timeline APIs         | Link FPO crop plans to activities later.                             |
| CORE-007 | Done   | Evidence             | Proof upload, storage, review                 | Reuse for field photos and plot/crop proof.                          |
| CORE-008 | Done   | Audit                | Audit event service and table                 | Add FPO-specific audit actions as needed.                            |
| CORE-009 | Done   | Reporting            | Generic summary, PDF, XLSX export foundation  | Add FPO-specific report sheets and summary fields.                   |
| CORE-010 | Done   | Notifications        | Notification event/status foundation          | Add advisory semantics on top.                                       |
| CORE-011 | Done   | Storage              | Local and MinIO providers                     | Reuse for photos, reports, future documents.                         |
| CORE-012 | Done   | DevOps               | Docker, production config, CI, security scan  | Add FPO tests to existing CI.                                        |

## Immediate Next Sprint

These are the next tasks once we start FPO MVP development. Complete in this
order unless the product scope changes.

| Priority | Task ID       | Title                                                    | Owner Area          | Depends On      | Status  |
| -------: | ------------- | -------------------------------------------------------- | ------------------- | --------------- | ------- |
|        1 | FPO-000       | Freeze Phase 1 data dictionary and report formats        | Product/Tech Lead   | Client input    | Done    |
|        2 | FPO-101       | Add FPO base schema migration                            | Backend             | FPO-000         | Done    |
|        3 | FPO-102       | Add FPO member profile backend APIs                      | Backend             | FPO-101         | Done    |
|        4 | FPO-FE-101    | Replace generic participant list with farmer/member list | Frontend            | FPO-102         | Done    |
|        5 | FPO-201       | Add landholding and farm plot backend APIs               | Backend             | FPO-102         | Done    |
|        6 | FPO-FE-201    | Add landholding and plot UI with GPS fields              | Frontend            | FPO-201         | Done    |
|        7 | FPO-301       | Add crop catalog, season, history, and crop plan APIs    | Backend             | FPO-201         | Done    |
|        8 | FPO-FE-301    | Add crop planning UI                                     | Frontend            | FPO-301         | Done    |
|        9 | FPO-401       | Add input catalog, input rules, and demand calculation   | Backend             | FPO-301         | Done    |
|       10 | FPO-FE-401    | Add demand summary UI                                    | Frontend            | FPO-401         | Done    |
|       11 | FPO-501       | Add FPO dashboard summary API                            | Backend             | FPO-401         | Done    |
|       12 | FPO-502       | Add FPO Excel export sheets                              | Backend             | FPO-501         | Done    |
|       13 | FPO-FE-601    | Add advisory UI                                          | Frontend            | FPO-601         | Done    |
|       14 | CARBON-FE-001 | Add dummy carbon app-flow UI foundation                  | Frontend/Docs       | Client app flow | Done    |
|       15 | FPO-QA-001    | Add Phase 1 UAT and API smoke tests                      | QA/Backend/Frontend | FPO-502         | Partial |

## Phase 1 Scope Alignment Sprint

These are the next implementation tasks after the client decision lock. Pick
them in this order unless a production defect interrupts the work.

| Priority | Task ID | Title | Owner Area | Status |
| -------: | ------- | ----- | ---------- | ------ |
| 1 | FPO-ALIGN-001 | Replace legacy FPO role assumptions with `ADMIN`, `FPO_MANAGER`, and `FIELD_COORDINATOR` | Backend/Frontend/QA | Pending |
| 2 | FPO-ALIGN-002 | Add FPO ownership, scoped access, and role isolation tests | Backend/QA | Pending |
| 3 | FPO-ALIGN-003 | Align farmer profile fields: taluka/state, Aadhaar optional, status `Suspended`, category labels | Backend/Frontend/QA | Pending |
| 4 | FPO-ALIGN-004 | Add soil profile entry and optional report attachment without carbon calculation | Backend/Frontend/QA | Pending |
| 5 | FPO-ALIGN-005 | Align land/GPS labels and approved ownership/irrigation options | Backend/Frontend/QA | Pending |
| 6 | FPO-ALIGN-006 | Add crop plan `confirmed_at`, crop year string, and optional expected yield | Backend/Frontend/QA | Pending |
| 7 | FPO-ALIGN-007 | Apply input demand 5% buffer, round-up, and confirmed-only report filtering | Backend/Frontend/QA | Pending |
| 8 | FPO-ALIGN-008 | Refactor FPO Excel export to the approved three-sheet workbook | Backend/Frontend/QA | Pending |
| 9 | FPO-ALIGN-009 | Add advisory crop targeting and multiple image attachments through storage | Backend/Frontend/QA | Pending |
| 10 | FPO-ALIGN-010 | Convert UAT guide scenarios into smoke/integration coverage where practical | QA/Backend/Frontend | Pending |

## Module Platform Tasks

These tasks protect modular pricing and single-service delivery. Build them
before selling multiple independent packages or handing over any private build.

| ID      | Status  | Task                                                    | Area         | Depends On |
| ------- | ------- | ------------------------------------------------------- | ------------ | ---------- |
| MOD-001 | Done    | Add module code enum                                    | Backend      | None       |
| MOD-002 | Done    | Add platform module tables and seed data                | Backend/DB   | MOD-001    |
| MOD-003 | Done    | Add tenant module subscription service                  | Backend      | MOD-002    |
| MOD-004 | Done    | Add backend module guard and `MODULE_NOT_ENABLED` error | Backend      | MOD-003    |
| MOD-005 | Done    | Add enabled-module endpoint after login                 | Backend      | MOD-003    |
| MOD-006 | Done    | Add frontend enabled-module store                       | Frontend     | MOD-005    |
| MOD-007 | Done    | Hide disabled modules in navigation                     | Frontend     | MOD-006    |
| MOD-008 | Done    | Add backend tests for disabled module access            | QA/Backend   | MOD-004    |
| MOD-009 | Pending | Document source-handover packaging process              | Docs/Release | MOD-004    |

## Phase 0: Discovery Tasks

Do not start schema work until these tasks are answered. Without this, the team
will guess fields and formulas.

| ID      | Status  | Task                               | Output                                                   |
| ------- | ------- | ---------------------------------- | -------------------------------------------------------- |
| FPO-000 | Done | Freeze Phase 1 MVP scope           | Locked in the Phase 1 Client Decision Register.          |
| FPO-001 | Done | Confirm user roles                 | `ADMIN`, `FPO_MANAGER`, `FIELD_COORDINATOR`; no farmer login. |
| FPO-002 | Done | Confirm farmer/member fields       | Final member profile data dictionary is approved.        |
| FPO-003 | Done | Confirm land and plot fields       | GPS point-only land data dictionary is approved.         |
| FPO-004 | Done | Confirm crop list and season names | Crop catalog and season master list are approved.        |
| FPO-005 | Done | Confirm input catalog              | Seeds and fertilizers required; later types optional.    |
| FPO-006 | Done | Confirm input formulas             | Fixed per-acre, confirmed plans only, 5% buffer, round-up. |
| FPO-007 | Done | Confirm report formats             | Excel sheets, columns, filters, branding, and footer approved. |
| FPO-008 | Future | Confirm OTP/SMS provider          | Explicitly excluded from Phase 1.                        |
| FPO-009 | Done | Confirm map/GPS expectations       | GPS point capture only; no polygon/offline in Phase 1.   |
| FPO-010 | Done | Confirm first release language     | English only for Phase 1.                                |

Acceptance criteria:

- Data dictionary is written in docs.
- Formulas are written as examples.
- Report columns are approved.
- Phase 2 features are explicitly excluded from Phase 1.

## Phase 1A: FPO Base Data Model

### FPO-101: Add FPO Schema Migration

Status:

- Done.

Goal:

- Add first-class FPO tables instead of overloading generic user/activity fields.

Backend files/modules:

- `backend/src/main/resources/db/migration/V3__fpo_mvp_schema.sql`
- `backend/src/main/java/com/activityplatform/backend/fpo/**`

Tables to create:

- `fpo_member_profiles`
- `farm_landholdings`
- `farm_plots`
- `crop_catalog`
- `crop_seasons`
- `farmer_crop_history`
- `seasonal_crop_plans`
- `input_catalog`
- `crop_input_rules`
- `input_demand_estimates`

Implementation notes:

- Every durable table must include `tenant_id`.
- `fpo_member_profiles.user_id` should reference `users.id`.
- Use UUID primary keys.
- Add `created_at` and `updated_at` to mutable tables.
- Add indexes for `tenant_id`, `member_profile_id`, `crop_id`, `season_id`,
  `village`, and `status` where relevant.
- Use `ON DELETE` carefully. Avoid deleting business history automatically.

Tests:

- Flyway migration validates.
- JPA schema validates.
- Integration test starts with PostgreSQL Testcontainers.

Acceptance criteria:

- Flyway migration exists for all Phase 1 FPO foundation tables.
- JPA compile validates the new member entity mapping.
- Targeted FPO integration tests are added. They require Docker/Testcontainers
  to be reachable when executed.
- Database guide and diagrams are updated.

### FPO-102: Add Member Profile Backend APIs

Status:

- Done.

Goal:

- Manage FPO farmer/member records linked to platform users.

Backend endpoints:

- `GET /api/v1/fpo/members`
- `POST /api/v1/fpo/members`
- `GET /api/v1/fpo/members/{memberId}`
- `PUT /api/v1/fpo/members/{memberId}`
- `PATCH /api/v1/fpo/members/{memberId}/status`

Request fields:

- `userId` or create-user payload.
- `memberNumber`.
- `displayName`.
- `mobileNumber`.
- `alternateMobileNumber`.
- `village`.
- `blockName`.
- `districtName`.
- `gender`.
- `dateOfBirth` or `age`.
- `farmerCategory`.
- `coordinatorUserId`.
- `status`.

Backend implementation tasks:

- Add entity, repository, DTOs, service, controller.
- Add validation for mobile number and member number.
- Enforce tenant isolation.
- Allow admin/supervisor writes.
- Allow participant to read own member profile.
- Emit audit events:
  - `FPO_MEMBER_CREATED`
  - `FPO_MEMBER_UPDATED`
  - `FPO_MEMBER_STATUS_CHANGED`

Tests:

- Admin can create member.
- Supervisor can create member if business rule allows.
- Participant cannot create other member.
- Duplicate member number returns conflict.
- Duplicate mobile number returns conflict if mobile uniqueness is confirmed.
- Tenant isolation works.

Acceptance criteria:

- Admin/supervisor can create, list, update, and change member status through
  `/api/v1/fpo/members`.
- Participant can read their own profile through `/api/v1/fpo/members/me`.
- Member can be linked to an existing participant user or a newly created
  participant account.
- `MEMBER_DATA` module guard returns `MODULE_NOT_ENABLED` when disabled.
- No demo member data appears unless seeded intentionally.

### FPO-FE-101: Add Farmer/Member Management UI

Status:

- Done.

Goal:

- Replace generic participant management with FPO member management in admin UI.

Frontend files likely touched:

- `src/screens/AdminHomeScreen.tsx`
- new `src/screens/AdminMembersTab.tsx`
- `src/data/adminRegistryStore.ts` or new `src/data/fpoMemberStore.ts`
- `src/core/api/endpoints.ts`
- `src/core/api/contracts.ts`

Frontend tasks:

- Done: Add FPO member store backed by `/api/v1/fpo/members`.
- Done: Replace visible admin participant management labels with FPO
  member/farmer terminology.
- Done: Add member list with search.
- Done: Add member create and inline edit forms.
- Done: Show linked username, member number, mobile number, village,
  block/district, farmer category, age, and status.
- Done: Add activate/deactivate action through the member status API.
- Done: Keep backend contracts behind reusable store functions.
- Pending: Add a fuller member detail page when landholding/plot UI is added.
- Pending: Add profile-completeness scoring after the final client data
  dictionary is frozen.

Tests/checks:

- `npm run typecheck`.
- `npm run lint`.
- Manual browser test: create, edit, list, inactive state.

Acceptance criteria:

- Admin can create a farmer/member from UI.
- New member appears from backend after refresh.
- Admin can update member profile fields from UI.
- Admin can search members by name, member number, mobile number, village, or
  username.
- Duplicate errors show useful text.

## Phase 1B: Landholding And Farm Plots

### FPO-201: Add Landholding APIs

Status:

- Done.

Backend endpoints:

- `GET /api/v1/fpo/members/{memberId}/landholdings`
- `POST /api/v1/fpo/members/{memberId}/landholdings`
- `PUT /api/v1/fpo/landholdings/{landholdingId}`
- `PATCH /api/v1/fpo/landholdings/{landholdingId}/status`

Fields:

- `memberId`.
- `surveyNumber`.
- `totalAreaAcres`.
- `cultivableAreaAcres`.
- `ownershipType`.
- `irrigationSource`.
- `status`.

Implementation notes:

- APIs are guarded by the `LAND_RECORDS` module.
- Admins and supervisors can create/update/status-change records.
- Participants can read their own member landholdings.
- Status changes are soft lifecycle changes: `ACTIVE`, `INACTIVE`, or
  `ARCHIVED`.
- Audit actions are recorded for create, update, and status change.

Tests:

- Cannot manage landholding for another tenant.
- Area must be positive.
- Cultivable area cannot exceed total area.
- Disabled `LAND_RECORDS` returns `MODULE_NOT_ENABLED`.
- Service unit tests cover validation and access rules.

Acceptance criteria:

- Landholding totals can be shown per member.
- Backend returns stable list/detail payloads for the upcoming UI.

### FPO-202: Add Farm Plot APIs

Status:

- Done.

Backend endpoints:

- `GET /api/v1/fpo/members/{memberId}/plots`
- `POST /api/v1/fpo/members/{memberId}/plots`
- `PUT /api/v1/fpo/plots/{plotId}`
- `PATCH /api/v1/fpo/plots/{plotId}/status`

Fields:

- `plotName`.
- `areaAcres`.
- optional `landholdingId`.
- `latitude`.
- `longitude`.
- `soilType`.
- `status`.

Validation:

- Latitude between `-90` and `90`.
- Longitude between `-180` and `180`.
- Area positive.
- Plot must belong to the member's tenant.
- If a plot references a landholding, that landholding must belong to the same
  member.

Tests:

- Valid coordinate accepted.
- Invalid coordinate rejected.
- Participant can view own plots if required.
- Admin/supervisor can manage plots.
- Service unit tests reject cross-member landholding assignment.

Acceptance criteria:

- Plot data supports acreage and geo-tagging reports.
- Backend returns stable plot payloads for the upcoming UI.

### FPO-FE-201: Add Landholding And Plot UI

Status:

- Done.

Frontend tasks:

- Done: Add collapsible landholding/plot panel to each admin member card.
- Done: Add reusable `farmAssetStore` backed by
  `/api/v1/fpo/members/{memberId}/landholdings` and
  `/api/v1/fpo/members/{memberId}/plots`.
- Done: Add landholding create form, saved landholding list, and lifecycle
  status actions.
- Done: Add plot create form, saved plot list, optional linked landholding,
  and lifecycle status actions.
- Done: Allow manual latitude/longitude entry for admin web use.
- Done: Capture current browser/device coordinates where `navigator.geolocation`
  is available.
- Done: Show total active plot area.
- Done: Show GPS permission/unavailable errors clearly and keep manual entry as
  the fallback.
- Pending: Add edit-in-place forms for existing landholding/plot records if
  client UAT needs correction workflows beyond archive/reactivate.

Acceptance criteria:

- Admin can add plots for a farmer.
- GPS values are visible after save.
- App works even if GPS permission is denied.
- Frontend typecheck and lint pass.

## Phase 1C: Crop History And Seasonal Planning

### FPO-301: Add Crop Catalog APIs

Status:

- Done.

Endpoints:

- `GET /api/v1/fpo/crops`
- `POST /api/v1/fpo/crops`
- `PUT /api/v1/fpo/crops/{cropId}`
- `PATCH /api/v1/fpo/crops/{cropId}/status`

Fields:

- `code`.
- `name`.
- `category`.
- `status`.

Implementation notes:

- APIs are guarded by the `CROP_PLANNING` module.
- Admins and supervisors can create, update, and change lifecycle status.
- Crop codes are normalized to uppercase and unique per tenant.
- Lifecycle status values are `ACTIVE`, `INACTIVE`, and `ARCHIVED`.
- Audit actions:
  - `FPO_CROP_CREATED`
  - `FPO_CROP_UPDATED`
  - `FPO_CROP_STATUS_CHANGED`

Acceptance criteria:

- Done: Admin/supervisor can maintain tenant crop master records.
- Done: Disabled `CROP_PLANNING` returns `MODULE_NOT_ENABLED`.
- Done: Integration and service tests cover create/update/status, validation,
  and module-disabled behavior.

### FPO-302: Add Season APIs

Status:

- Done.

Endpoints:

- `GET /api/v1/fpo/seasons`
- `POST /api/v1/fpo/seasons`
- `PUT /api/v1/fpo/seasons/{seasonId}`
- `PATCH /api/v1/fpo/seasons/{seasonId}/status`

Fields:

- `code`.
- `name`.
- `seasonYear`.
- `startMonth`.
- `endMonth`.
- `status`.

Implementation notes:

- APIs are guarded by the `CROP_PLANNING` module.
- Season code plus year is unique per tenant.
- Start and end month are optional, but must be provided together.
- Audit actions:
  - `FPO_SEASON_CREATED`
  - `FPO_SEASON_UPDATED`
  - `FPO_SEASON_STATUS_CHANGED`

Acceptance criteria:

- Done: Crop plans can be grouped by season.

### FPO-303: Add Crop History APIs

Status:

- Done.

Endpoints:

- `GET /api/v1/fpo/members/{memberId}/crop-history`
- `POST /api/v1/fpo/members/{memberId}/crop-history`
- `PUT /api/v1/fpo/crop-history/{historyId}`

Fields:

- `cropId`.
- `seasonId`.
- `cropYear`.
- `areaAcres`.
- `yieldQuantity`.
- `yieldUnit`.
- `notes`.

Implementation notes:

- APIs are guarded by the `CROP_PLANNING` module.
- Admins and supervisors can create/update crop history.
- Participants can read their own member crop history.
- New history must reference active crop and active season when a season is
  provided.
- Audit actions:
  - `FPO_CROP_HISTORY_CREATED`
  - `FPO_CROP_HISTORY_UPDATED`

Acceptance criteria:

- Done: Admin/supervisor can record previous crops per farmer.
- Done: Participant can read own crop history.

### FPO-304: Add Seasonal Crop Plan APIs

Status:

- Done.

Endpoints:

- `GET /api/v1/fpo/crop-plans`
- `POST /api/v1/fpo/crop-plans`
- `GET /api/v1/fpo/crop-plans/{planId}`
- `PUT /api/v1/fpo/crop-plans/{planId}`
- `PATCH /api/v1/fpo/crop-plans/{planId}/status`

Fields:

- `memberId`.
- `plotId`.
- `cropId`.
- `seasonId`.
- `plannedAreaAcres`.
- `plannedSowingDate`.
- `expectedHarvestDate`.
- `status`.

Optional link:

- `activityId` if a crop plan starts a workflow activity.

Validation:

- Planned acreage cannot exceed active plot area unless override is allowed.
- Crop and season must be active.
- If a plot is provided, it must belong to the selected member and be active.
- Expected harvest date cannot be before planned sowing date.

Status values:

- `DRAFT`
- `CONFIRMED`
- `CANCELLED`
- `COMPLETED`

Audit actions:

- `FPO_CROP_PLAN_CREATED`
- `FPO_CROP_PLAN_UPDATED`
- `FPO_CROP_PLAN_STATUS_CHANGED`

Acceptance criteria:

- Done: Admin can see crop plan coverage by crop, village, and season.
- Done: Admin/supervisor can create, update, view, filter, and change plan
  status.
- Done: Participants can read their own crop plans.

### FPO-FE-301: Add Crop Planning UI

Status:

- Done.

Frontend tasks:

- Done: Add crop catalog admin screen inside the admin Crop Planning tab.
- Done: Add season setup screen inside the same tab.
- Done: Add member crop history section.
- Done: Add seasonal crop plan form.
- Done: Add filters for season, crop, and status.
- Done: Add summary cards for planned acreage, farmer count, village count,
  and plan count.
- Done: Add reusable `cropPlanningStore` backed by crop planning APIs with
  prototype cache fallback.

Acceptance criteria:

- Done: Admin can create seasonal plan and see it in summary.
- Done: Frontend typecheck passes after wiring.

## Phase 1D: Input Demand Engine

### FPO-401: Add Input Catalog APIs

Status:

- Done.

Endpoints:

- `GET /api/v1/fpo/inputs`
- `POST /api/v1/fpo/inputs`
- `PUT /api/v1/fpo/inputs/{inputId}`
- `PATCH /api/v1/fpo/inputs/{inputId}/status`

Fields:

- `code`.
- `name`.
- `category`.
- `unit`.
- `status`.

Implementation notes:

- APIs are guarded by the `INPUT_DEMAND` module.
- Admins and supervisors can create, update, list, and change lifecycle status.
- Input codes and units are normalized to uppercase.
- Input codes are unique per tenant.
- Lifecycle status values are `ACTIVE`, `INACTIVE`, and `ARCHIVED`.
- Audit actions:
  - `FPO_INPUT_CREATED`
  - `FPO_INPUT_UPDATED`
  - `FPO_INPUT_STATUS_CHANGED`

Acceptance criteria:

- Done: Admin/supervisor can maintain tenant input master records.
- Done: Disabled `INPUT_DEMAND` returns `MODULE_NOT_ENABLED`.
- Done: Service tests cover normalization and audit event metadata.

### FPO-402: Add Crop Input Rule APIs

Status:

- Done.

Endpoints:

- `GET /api/v1/fpo/input-rules`
- `POST /api/v1/fpo/input-rules`
- `PUT /api/v1/fpo/input-rules/{ruleId}`
- `PATCH /api/v1/fpo/input-rules/{ruleId}/status`

Fields:

- `cropId`.
- `inputId`.
- `quantityPerAcre`.
- `applicationStage`.
- `notes`.
- `status`.

Implementation notes:

- APIs are guarded by the `INPUT_DEMAND` module.
- Rules require active crop and active input records.
- Duplicate rules are blocked per tenant, crop, input, and application stage.
- Multiple stages for the same crop/input are allowed and are summed during
  demand calculation.
- Audit actions:
  - `FPO_INPUT_RULE_CREATED`
  - `FPO_INPUT_RULE_UPDATED`
  - `FPO_INPUT_RULE_STATUS_CHANGED`

Acceptance criteria:

- Done: Admin/supervisor can define how much input is needed per acre for a
  crop.
- Done: Inactive inputs are rejected by service-level validation tests.

### FPO-403: Add Demand Calculation Service

Status:

- Done.

Endpoint:

- `POST /api/v1/fpo/demand-estimates/run`

Request:

- `seasonId`.
- optional `cropId`.
- optional `village`.
- optional `planStatus`, defaulting to `CONFIRMED`.

Calculation:

```text
requiredQuantity = plannedAcreage * quantityPerAcre
```

Persist:

- Crop plan.
- Input.
- Estimated quantity.
- Unit.
- Estimate status.

Tests:

- Done: Calculation matches expected numeric examples.
- Done: Multiple active stages for the same input are summed into one estimate
  per crop plan and input.
- Done: Missing rules are counted and skipped without creating bad estimates.
- Done: Controller integration coverage is present, but requires Docker/
  Testcontainers to be reachable when executed.

Acceptance criteria:

- Done: Demand can be generated from confirmed crop plans.
- Done: Calculation emits `FPO_INPUT_DEMAND_CALCULATED`.

### FPO-404: Add Demand Summary APIs

Status:

- Done.

Endpoints:

- `GET /api/v1/fpo/demand-estimates`
- `GET /api/v1/fpo/demand-estimates/summary`

Summaries:

- By crop.
- By input.
- By village.
- By season.
- Row-level estimates include farmer/member, crop, season, village, input, unit,
  status, and quantity.

Acceptance criteria:

- Done: Admin dashboard can show consolidated FPO demand by season, crop, and
  village filters.

### FPO-FE-401: Add Input Demand UI

Status:

- Done.

Frontend tasks:

- Done: Input catalog management screen.
- Done: Input rule management screen.
- Done: Demand calculation run button.
- Done: Demand summary view by input, crop, and village.
- Done: Farmer-wise demand estimate list.
- Done: Reusable `inputDemandStore` backed by `/api/v1/fpo/inputs`,
  `/api/v1/fpo/input-rules`, and `/api/v1/fpo/demand-estimates`.
- Pending: FPO-specific export button belongs to FPO-FE-501 report UI work.

Acceptance criteria:

- Done: Admin can run calculation and understand results without database
  access.

## Phase 1E: Reports And Exports

### FPO-501: Add FPO Dashboard Summary

Status:

- Done.

Endpoint:

- `GET /api/v1/fpo/reports/summary`

Metrics:

- Total members.
- Active members.
- Total land area.
- Total active plot area.
- Geo-tagged plot count.
- Crop-wise planned acreage.
- Village-wise planned acreage.
- Input-wise demand totals.
- Farmer coverage by season.
- Input-wise demand totals by unit.

Tests:

- Done: Unit test covers member, land, plot, crop plan, and input demand
  aggregation.
- Done: Controller integration test source covers endpoint response and module
  disabled behavior. Execution requires Docker/Testcontainers to be reachable.

Acceptance criteria:

- Done: Dashboard API can answer basic FPO planning questions from durable FPO
  tables.
- Done: API is guarded by `REPORT_EXPORT`.
- Done: API emits `FPO_REPORT_SUMMARY_VIEWED`.

### FPO-502: Add FPO Excel Export

Status:

- Done.

Endpoint:

- `POST /api/v1/fpo/reports/export`

Workbook sheets:

- `Farmer Master`.
- `Landholdings`.
- `Farm Plots`.
- `Crop History`.
- `Seasonal Crop Plans`.
- `Input Demand Summary`.
- `Farmer-wise Input Demand`.

Tests:

- Done: Workbook is generated as a valid XLSX package.
- Done: Sheet names match the Phase 1 export spec.
- Done: Required columns exist for farmer, land, plot, crop history, crop
  plan, input demand summary, and farmer-wise input demand sheets.
- Pending: Row-count checks against database fixtures can be added with the
  broader API smoke/UAT catalog.

Acceptance criteria:

- Done: Export file contains the FPO/admin/government sheets needed for Phase
  1 review.

### FPO-FE-501: Add FPO Dashboard And Export UI

Status:

- Done.

Frontend tasks:

- Add summary cards.
- Add charts or dense tables for crop/input summaries.
- Add season/crop/village filters.
- Add export buttons.
- Show generated report status and storage key.

Acceptance criteria:

- Admin can review and export FPO reports from UI.

## Phase 1F: Basic Advisory And Notifications

### FPO-601: Add Advisory Records

Status:

- Done.

Backend endpoints:

- Done: `POST /api/v1/fpo/advisories`
- Done: `GET /api/v1/fpo/advisories`
- Done: `GET /api/v1/fpo/advisories/{advisoryId}`
- Done: `PATCH /api/v1/fpo/advisories/{advisoryId}/status`

Fields:

- `cropId`.
- `seasonId`.
- `targetType`.
- `targetVillage`.
- `targetMemberId`.
- `title`.
- `message`.
- `channel`.
- `status`.

Phase 1 rule:

- Done: Store advisories for in-app use.
- Done: Keep SMS/WhatsApp delivery out of Phase 1 provider scope.
- Done: Guard APIs with the `ADVISORY` module.
- Done: Allow admin/supervisor to create and publish advisories.
- Done: Allow participants to view only published advisories targeted to all
  members, their village, or their own member profile.
- Done: Audit advisory create and status changes.

Acceptance criteria:

- Done: Admin can publish an advisory.
- Done: Farmer can view relevant advisories through backend APIs.

### FPO-FE-601: Add Advisory UI

Status:

- Done.

Frontend tasks:

- Done: Admin advisory create/list screen.
- Done: Farmer advisory list screen inside the carbon tab.
- Done: Crop/season context and all-member/village/member targeting controls.
- Done: Status filters, publish action, archive action, empty states, and
  local dummy fallback.

Acceptance criteria:

- Done: Relevant advisory is visible through backend APIs where available, with
  dummy local data used for prototype mode.

## Client Carbon App Flow Tasks

These tasks translate the client's carbon accounting app flow into the current
platform base. Provider-dependent items remain blocked until commercial and
technical decisions are approved.

| ID             | Status  | Task                                      | Notes                                                                              |
| -------------- | ------- | ----------------------------------------- | ---------------------------------------------------------------------------------- |
| CARBON-DOC-001 | Done    | Create carbon app-flow base plan          | See `docs/carbon-app-flow-base-plan.md`.                                           |
| CARBON-FE-001  | Done    | Add dummy admin carbon operations tab     | Covers identity, soil, activity verification, carbon potential, and dealers.       |
| CARBON-FE-002  | Done    | Add dummy farmer carbon tab               | Covers identity, soil score, activity scoring, advisories, and nearby dealers.     |
| CARBON-BE-001  | Pending | Draft carbon schema/data dictionary       | Wait for formula, soil fields, and methodology confirmation before durable schema. |
| CARBON-BE-002  | Pending | Add soil profile APIs                     | Can start after schema draft is accepted internally.                               |
| CARBON-BE-003  | Pending | Add provisional carbon calculator service | Formula must remain flagged as provisional until client approval.                  |
| CARBON-BE-004  | Pending | Add dealer directory APIs                 | Dummy records are enough until dealer onboarding data is supplied.                 |
| CARBON-INT-001 | Blocked | OTP/SMS login                             | Needs provider, pricing, sender ID, and templates.                                 |
| CARBON-INT-002 | Blocked | Map boundary drawing                      | Needs map provider and boundary accuracy expectation.                              |
| CARBON-INT-003 | Blocked | AI verification                           | Needs provider and acceptance rules.                                               |
| CARBON-INT-004 | Blocked | Satellite layers                          | Needs NDVI/NDRE/NDMI provider and refresh rules.                                   |

## Phase 1G: OTP And Mobile Login

### FPO-701: OTP Provider Spike

Status:

- Blocked.

Blocked by:

- Client approval of SMS/OTP cost.
- Provider selection.
- Sender ID/template requirements.

Developer spike tasks:

- Decide provider abstraction.
- Define OTP TTL and retry policy.
- Define rate limit.
- Define fallback for admin username/password login.

Acceptance criteria:

- Technical design approved before implementation.

### FPO-702: Add OTP Auth Backend

Status:

- Blocked.

Endpoints:

- `POST /api/v1/auth/otp/request`
- `POST /api/v1/auth/otp/verify`

Security tasks:

- Hash OTP or store in short-lived secure store.
- Rate-limit mobile number and IP.
- Audit OTP request and verify.
- Do not log OTP.

Acceptance criteria:

- Farmer can log in by mobile number if provider is configured.

### FPO-FE-701: Add OTP Login UI

Status:

- Blocked.

Frontend tasks:

- Mobile number input screen.
- OTP verify screen.
- Retry countdown.
- Error handling for invalid/expired OTP.

Acceptance criteria:

- OTP login works on mobile web/app after provider setup.

## QA And Testing Tasks

| ID         | Status  | Task                         | Expected Output                                                                                                    |
| ---------- | ------- | ---------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| FPO-QA-001 | Partial | FPO API integration tests    | Testcontainers coverage exists for member, plot, crop plan, and demand; execution requires Docker to be reachable. |
| FPO-QA-002 | Done    | FPO service unit tests       | Calculation and validation tests for current FPO modules.                                                          |
| FPO-QA-003 | Partial | Frontend type/lint checks    | Commands exist and run in CI; rerun after each UI change.                                                           |
| FPO-QA-004 | Partial | Excel export verification    | Unit-level workbook fixture checks exist; database fixture row-count checks remain.                                |
| FPO-QA-005 | Pending | Role access matrix tests     | Admin/supervisor/farmer permissions.                                                                               |
| FPO-QA-006 | Pending | UAT test catalog             | Manual test script for client UAT.                                                                                 |
| FPO-QA-007 | Pending | Seed/demo data cleanup check | Confirm no old dummy farmer appears.                                                                               |

## Documentation Tasks

| ID          | Status  | Task                         | Expected Output                           |
| ----------- | ------- | ---------------------------- | ----------------------------------------- |
| FPO-DOC-001 | Done    | Add FPO architecture diagram | Component/data model docs include FPO tables and ownership. |
| FPO-DOC-002 | Done    | Add FPO database guide       | Database guide includes FPO relationships and module pattern. |
| FPO-DOC-003 | Partial | Add admin user guide         | Admin workflows exist; final client-facing manual still pending. |
| FPO-DOC-004 | Pending | Add UAT guide                | Step-by-step acceptance script.           |
| FPO-DOC-005 | Partial | Add deployment runbook       | Clean start, deployment, and security docs exist; target ops runbook remains. |

## Future Phase Tasks

These are not Phase 1 MVP tasks.

| ID      | Status | Future Phase | Task                                |
| ------- | ------ | ------------ | ----------------------------------- |
| FUT-201 | Future | Phase 2      | Input inventory and stock ledger.   |
| FUT-202 | Future | Phase 2      | Input distribution to farmers.      |
| FUT-203 | Future | Phase 2      | Produce procurement records.        |
| FUT-204 | Future | Phase 2      | Farmer ledger and payment tracking. |
| FUT-301 | Future | Phase 3      | Weather API integration.            |
| FUT-302 | Future | Phase 3      | Crop-stage advisory rule engine.    |
| FUT-303 | Future | Phase 3      | Agronomist review workflow.         |
| FUT-304 | Future | Phase 3      | WhatsApp Business API integration.  |
| FUT-305 | Future | Phase 3      | Voice advisory integration.         |
| FUT-401 | Future | Phase 4      | Produce lot creation.               |
| FUT-402 | Future | Phase 4      | Quality grading.                    |
| FUT-403 | Future | Phase 4      | QR traceability page.               |
| FUT-404 | Future | Phase 4      | Buyer portal.                       |
| FUT-501 | Future | Phase 5      | Satellite/NDVI integration.         |
| FUT-502 | Future | Phase 5      | IoT sensor ingestion.               |
| FUT-503 | Future | Phase 5      | Carbon/sustainability tracking.     |
| FUT-504 | Future | Phase 5      | Advanced analytics and forecasting. |

## Developer Working Rules

- Keep generic core reusable.
- Add FPO-specific behavior under a dedicated FPO module.
- Do not hardcode crop names in shared core.
- Add Flyway migrations for schema changes.
- Add integration tests for every new controller.
- Add service unit tests for calculation rules.
- Update diagrams after schema changes.
- Update this task list after each completed slice.
- Do not add WhatsApp, AI, IoT, payment gateway, marketplace, or satellite
  integration inside Phase 1 unless contract scope changes.
