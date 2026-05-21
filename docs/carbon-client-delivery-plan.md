# Carbon Client Delivery Plan

Last updated: 2026-05-21

Purpose: translate the client's `Carbon Flow.xlsx` into a delivery plan that
protects the reusable platform foundation while giving the client a clear
Carbon-first UAT path.

This is the project-manager source of truth for delivery sequencing. It links
to, but does not replace:

- [Carbon Flow Screen Status And Task Breakdown](carbon-flow-screen-status-task-breakdown.md)
  for screen-by-screen status.
- [Carbon Flow Client Excel Impact Analysis](carbon-flow-client-excel-impact-analysis.md)
  for what to keep, rework, and avoid.
- [Farmer Identity Foundation](farmer-identity-foundation.md) for canonical
  farmer identity rules.
- [Foundation Hardening Roadmap](foundation-hardening-roadmap.md) for
  cross-cutting reliability, QA, security, and reusable-platform hardening.

## PM Decision

We should proceed with the Carbon client app, but not as a full 77-screen build
in one pass.

The client Excel describes a large farmer mobile product. Our current codebase
already has a strong reusable foundation, but most client-specific screens are
not implemented as the Excel flow. The correct strategy is:

1. Keep the shared platform foundation.
2. Keep the FPO module intact and hidden for Carbon-only clients.
3. Treat `vineyard` as plot/block terminology inside Carbon, not as a separate
   module.
4. Deliver a focused Carbon UAT slice first.
5. Defer marketplace, wallet, satellite, AI diagnosis, and advanced support
   until the core farmer-to-admin carbon workflow is proven.

## Non-Negotiable Engineering Rules

- Farmer identity always resolves through:

```text
users -> farmer_profiles -> module extension records
```

- Carbon profile, plot, soil, activity, bank, document, wallet, and future
  records must link to the canonical farmer profile or to a Carbon enrollment
  that already links to it.
- Do not add another farmer identity table.
- Do not create a `vineyard` module unless it becomes an independently licensed
  product later.
- Do not delete or weaken the FPO module. Hide it with module visibility for
  Carbon-only packaging.
- Use the central role/module visibility registry for screen access.
- Use workflow/activity/evidence for farmer activities and proof.
- Use the storage adapter for documents, soil reports, activity evidence, and
  generated files.
- Add tests for new backend endpoints and critical frontend visibility logic.

## Delivery Shape

The 77 screens should be treated as three delivery bands.

| Band | Goal | Screens | Delivery Decision |
| ---- | ---- | ------- | ----------------- |
| P0 UAT Core | Client can demo the real Carbon farmer journey end to end. | 6-8, 11, 13-22, 27-34, 35-39, 68-71, 75-77 | Build first. This is the minimum meaningful client UAT. |
| P1 Product Depth | Make the app feel complete around advisory, reports, weather, settings, and support. | 1-5, 9-10, 12, 23-26, 40-44, 54-57, 61-67 | Build after P0 unless client declares any item mandatory for UAT. |
| P2 Commercial Expansion | Marketplace, wallet, certificates, dealer/admin/project expansion. | 45-53, 58-60, 72-74 | Plan now, build after core carbon methodology and business rules are locked. |

## P0 UAT Core Journey

This is the first target we should build and stabilize.

```text
Staff/Admin creates or verifies farmer
  -> farmer logs in
  -> farmer sees Carbon dashboard
  -> farmer completes profile/bank/docs
  -> farmer adds vineyard plot/block
  -> farmer adds soil profile/report
  -> farmer records Carbon activity through wizard
  -> farmer uploads evidence
  -> admin reviews activity/evidence/soil
  -> basic Carbon report/export is visible
```

P0 does not require real SMS, real satellite layers, real AI, real payments, or
verified carbon credit issuance. Those can be mock/placeholders with honest UI
labels until the client locks providers and methodology.

## Sprint Plan

| Sprint | Target | Screen Coverage | Backend Scope | Frontend Scope | Exit Criteria |
| ------ | ------ | --------------- | ------------- | -------------- | ------------- |
| S0 | Stabilize foundation before client build | Cross-cutting | Finish `FOUNDATION-QA-001`; verify Carbon-only, FPO-only, and combined package flows; confirm no old FPO-gated farmer activity path remains. | Verify farmer dashboard/activity tabs are common and module-visible correctly. | Farmer created from Carbon can log in and see assigned workflow activities. |
| S1 | Carbon app shell and navigation | 11, 75-77 plus shell for P0 screens | No new domain tables unless needed for navigation metadata. | Rework Carbon UI shell into dashboard, profile, plots, soil, activities, admin sections; add shared success/error/logout confirmation patterns. | User can move through the P0 app structure without FPO screens showing in Carbon-only mode. |
| S2 | Auth and farmer profile UAT flow | 6-8, 13-15 | Add OTP placeholder tables/endpoints if approved; add `farmer_bank_details`; add `farmer_documents`; expose profile completion/verification status. | Mobile/OTP UI with mock OTP or username/password fallback; profile wizard; bank form; document upload flow. | Farmer identity remains canonical; bank/docs do not duplicate Carbon profile identity fields. |
| S3 | Vineyard plot/block management | 16-19 | Extend `carbon_farm_plots` with vineyard/block fields such as variety, rootstock, planting year/date, block code, spacing, row count; keep `boundary_geojson`. | Farm plot list, add/edit block, detail view, map placeholder or polygon UI depending on provider decision. | Farmer/admin can create and view vineyard blocks linked to the same Carbon farmer identity. |
| S4 | Soil dashboard and reports intake | 20-22, 71 partial | Extend soil profile status/OCR placeholder if needed; prepare soil verification status. | Soil dashboard, report upload, manual entry, upload/processing/error states. | Soil data and reports are visible to farmer and admin without duplicate tables. |
| S5 | Activity wizard using workflow/evidence | 27-39, 70 partial | Seed Carbon/vineyard workflow definitions; add activity detail metadata for pruning, irrigation, spray, compost, cover crop; reuse evidence upload/review. | Multi-step wizard: block, crop/variety, activity type, details, evidence, mock verification, result. | Activity appears for farmer, evidence is uploaded, admin can approve/reject. |
| S6 | Admin UAT dashboard and verification | 68-71 | Add admin queues for farmer, activity/evidence, and soil verification; role guards for Admin/FPO Manager/Field Coordinator where needed. | Admin Carbon dashboard, farmer management, activity verification queue, soil verification queue. | Client owner can review pilot data and approve/reject UAT records. |
| S7 | Basic reports and UAT polish | 54 partial, UAT set | Add Carbon report endpoints/templates for farmer register, plot/soil summary, activity/evidence summary; use existing report exporter. | Reports dashboard with filters/downloads; loading/empty/error polish; UAT seed data. | P0 UAT script can be run end to end without developer help. |

## Backlog After P0

| Epic | Screens | Delivery Band | Decision |
| ---- | ------- | ------------- | -------- |
| Onboarding, language, consent, import | 1-5, 9-10 | P1 | Important for polished mobile release, not required to prove core data/workflow. |
| Notifications, weather, settings, support, offline sync | 12, 61-67 | P1 | Build after core APIs. Weather/offline need provider and product decisions. |
| Crop stage and season calendar | 25-26 | P1 | Useful scheduling layer over activities; depends on activity definitions. |
| Advisory and Ask Expert | 40-44 | P1 | Can reuse storage/notifications; needs expert ownership and mock AI scope. |
| Soil satellite layer | 23 | P1/P2 | Requires map/satellite provider. Do not block P0. |
| Soil recommendation | 24 | P1 | Needs recommendation formula/rules. Can start as static rules after soil dashboard. |
| Marketplace | 45-50 | P2 | Separate commercial module candidate. Needs dealer/order/payment decisions. |
| Carbon calculator | 51-53 | P2 unless client insists | Build only with dummy label until methodology is locked. Avoid verified-credit claims. |
| Wallet and certificate | 58-60 | P2 | Depends on calculator, verification, payout, and certificate rules. |
| Carbon project, dealer, agronomist admin | 72-74 | P2 | Depends on marketplace/advisory/project model decisions. |

## Immediate Task List

Start here once the user approves implementation.

| ID | Priority | Task | Depends On | Reuse |
| -- | -------- | ---- | ---------- | ----- |
| CARBON-CLIENT-001 | P0 | Confirm Carbon-only module visibility and remove any remaining FPO-gated farmer activity UI path. | Current foundation branch | `roleAccess.ts`, module registry, farmer participant endpoint |
| CARBON-CLIENT-002 | P0 | Create Carbon app shell/navigation aligned to P0 journey. | 001 | Existing Carbon module, shared UI patterns |
| CARBON-CLIENT-003 | P0 | Add profile-completion model: farmer profile, Carbon enrollment, bank status, document status. | 001 | `farmer_profiles`, `carbon_profiles`, storage |
| CARBON-CLIENT-004 | P0 | Add bank details table/API/UI. | 003 | `FarmerService`, audit, role guards |
| CARBON-CLIENT-005 | P0 | Add farmer document table/API/UI with storage. | 003 | storage adapter, upload patterns |
| CARBON-CLIENT-006 | P0 | Extend Carbon farm plots for vineyard block fields. | 003 | `carbon_farm_plots`, `boundary_geojson` |
| CARBON-CLIENT-006A | P0 | Refactor shared UI reuse before plot-detail work: soil report uploader, evidence uploader/review actions, and activity timeline components shared across Carbon/FPO where data models are not coupled. | 006 | `StateCard`, `StatusBadge`, upload adapter, evidence APIs |
| CARBON-CLIENT-007 | P0 | Build plot list/add/detail UI with map placeholder first. | 006A | Carbon plot APIs, shared activity/list UI patterns |
| CARBON-CLIENT-008 | P0 | Promote soil profile into dedicated dashboard/upload/manual entry screens. | 003 | `carbon_soil_profiles`, storage |
| CARBON-CLIENT-008B | P0 | Extract FPO soil UI to shared manual-entry and soil-list components, then reuse them from Carbon soil screens without changing activity/workflow code. | 008 | Shared soil UI components, FPO/Carbon soil adapters |
| CARBON-CLIENT-009 | P0 | Seed vineyard workflow definitions for core activities. | 001 | workflow definitions/tasks |
| CARBON-CLIENT-010 | P0 | Build activity wizard and evidence submission. | 006, 009 | activity, task, evidence APIs |
| CARBON-CLIENT-011 | P0 | Build admin verification queues for farmer/activity/soil. | 004, 005, 008, 010 | admin shell, evidence review |
| CARBON-CLIENT-012 | P0 | Add basic Carbon reports/export dashboard. | 008, 010, 011 | report exporter, XLSX/PDF builders |
| CARBON-CLIENT-013 | P0 | Add UAT seed data and UAT script. | 012 | test data factory, docs |
| CARBON-CLIENT-014 | P0 | Add regression tests for P0 user journey. | 013 | Testcontainers, module visibility smoke |

## Implementation Status

| ID | Status | Notes |
| -- | ------ | ----- |
| CARBON-CLIENT-001 | Done | Carbon-only visibility hides FPO navigation and keeps FPO code intact. |
| CARBON-CLIENT-002 | Done | Carbon shell now follows the P0 dashboard/profile/plots/soil/activities/admin shape. |
| CARBON-CLIENT-003 | Done | Profile completion endpoint and farmer widget are in place; real table-backed steps are enabled as later slices land. |
| CARBON-CLIENT-004 | In review | Bank details use the canonical farmer profile path with `PENDING_VERIFICATION` status; MVP stores account numbers in plain text and production must encrypt or tokenize bank identifiers before shared/live use. |
| CARBON-CLIENT-005 | Done | Farmer document upload and verification use the shared farmer document path with storage-backed PDF/image intake. |
| CARBON-CLIENT-006 | Done | Carbon farm plots include vineyard/block fields and staff/farmer UI can create and view linked blocks. |
| CARBON-CLIENT-006A | Done | Added shared UI components for soil report metadata/upload, evidence upload/review actions, and activity timelines; Carbon/FPO keep separate API stores and module guards. |
| CARBON-CLIENT-007 | Done | Carbon plot list now has selectable detail cards and map placeholders without adding a map provider dependency. |
| CARBON-CLIENT-008 | Done | Carbon Soil tab is now a dedicated dashboard/upload/manual-entry workspace using shared soil uploader/dashboard components; linked farmers can create/update/upload their own soil records while staff access remains scoped. |
| CARBON-CLIENT-008B | Done | FPO and Carbon now share soil manual-entry and soil-list UI components while preserving separate API stores and leaving activity/workflow code untouched. |
| CARBON-CLIENT-009 | Done | Vineyard workflow definitions are seeded under the Carbon workflow domain for core regenerative activities. |
| CARBON-CLIENT-010 | Done | Farmer Carbon activity entry uses the shared workflow wizard and submits evidence through the generic evidence table. |
| CARBON-CLIENT-011 | Done | Admin Carbon verification now includes farmer bank/document queues, Carbon workflow evidence review, and soil profile verification with approve/reject status. |
| CARBON-CLIENT-012 | Done | Carbon reports now include staff summary metrics, village/activity breakdowns, and scoped XLSX export over profile, plot, soil, activity, and verification state without credit calculations. |

## Client Decisions Needed Before Each Sprint

| Decision | Needed By | Recommendation If No Answer |
| -------- | --------- | --------------------------- |
| 76 vs 77 screens: keep Logout Confirmation as separate? | S1 | Keep it as shared logout modal. |
| OTP for farmers only or also staff/admin? | S2 | Farmers get OTP placeholder; staff/admin keep username/password. |
| Mock OTP acceptable for UAT? | S2 | Use mock OTP and document it clearly. |
| KYC document types | S2 | Start with Aadhaar, land document, soil report, bank proof, other. |
| Bank verification needed or store-only? | S2 | Store-only with `PENDING_VERIFICATION` status. |
| Map provider and polygon drawing requirement | S3 | Build map placeholder and `boundary_geojson` field first; add provider after decision. |
| OCR required for first UAT? | S4 | Store report and show `OCR_PENDING` placeholder only. |
| AI verification required for first UAT? | S5 | Use deterministic mock verification result; no AI claims. |
| Carbon methodology/formula | S7/P2 | Do not calculate verified credits; show estimate only if approved. |

## Definition Of Done

Every sprint must meet these before it is called complete:

- Backend migrations are Flyway versioned and reversible by environment reset.
- New APIs follow the response envelope and role/module guard patterns.
- Farmer-related records resolve through `FarmerService` or canonical farmer
  profile references.
- Frontend screens respect `EXPO_PUBLIC_ENABLED_CLIENT_MODULES` and
  `roleAccess.ts`.
- Loading, empty, error, and access-denied states are present for new screens.
- Storage flows do not leave silent orphan records on failed upload.
- Developer-facing docs are updated if behavior or setup changes.
- User runs the relevant test commands listed in the QA guide before commit.

## Test Commands For User-Run Verification

Use these after each implementation sprint:

```powershell
npm run typecheck
npm run lint
npm run test:module-visibility
cd backend
.\mvnw.cmd test
```

Use these before UAT release rehearsal:

```powershell
docker compose up -d
npm run typecheck
npm run lint
npm run test:module-visibility
cd backend
.\mvnw.cmd verify
```

## Go-Live Readiness Checklist

P0 can move to UAT only when:

- Carbon-only mode hides FPO screens cleanly.
- Admin can create or verify a farmer.
- Farmer can log in using the selected UAT auth method.
- Farmer profile, bank, documents, plot, and soil screens work end to end.
- Farmer can create at least one activity with evidence.
- Admin can review evidence and soil/report status.
- Basic reports/export are available.
- Seed data exists for at least five farmers and several vineyard plots.
- Critical known issues are documented in the gap register.
- Production secrets, storage, backup, and restore plan are documented before
  real production data is accepted.

## PM Recommendation For Next Work

Start with `CARBON-CLIENT-001` and `CARBON-CLIENT-002`.

Reason: before adding bank/docs/plots/soil screens, the app shell must stop
feeling like an older Carbon prototype or hidden FPO workflow. Once navigation
is clean, every later screen has a stable home and we avoid another round of
rigid duplicated UI.
