# FPO MVP Roadmap And Developer Task Plan

This is an internal planning document. It should not be shared with a client
before a signed agreement because it includes our current implementation status,
engineering assumptions, and delivery strategy.

For the developer checklist, use
[FPO Developer Task List](fpo-developer-task-list.md). This roadmap explains
why the phases exist; the task list explains what to build next.

For single-service client packaging, source handover risk, module flags, and
why the platform should stay a modular monolith before any microservice split,
see [Modular Platform Strategy](modular-platform-strategy.md).

## Product Positioning

The client POC describes a long-term FPO digitization platform. It is closer to
an enterprise AgTech SaaS roadmap than a single MVP. A mature platform in this
space usually combines:

- Farmer/member data collection.
- Plot and crop records.
- Field coordinator workflows.
- Agronomy/advisory support.
- Weather, satellite, soil, and optional IoT data.
- Transaction/procurement/inventory systems.
- Traceability and buyer/supply-chain reports.
- Admin dashboards and exports.

The practical first release should be:

```text
FPO Member Data, Crop Planning, Geo-Tagging, And Input Demand MVP
```

The current repository already contains a strong reusable activity-compliance
foundation, but it is not yet a complete FPO MVP. The next development work is
to add the FPO-specific data model, screens, reports, and computations on top of
the generic platform.

## Current Implementation Status

For the live module completion percentages, testing status, and go-live gaps,
use [Project Status And Gap Register](project-status-and-gap-register.md). This
roadmap keeps the product sequence and business framing only.

### Already Built

Backend foundation:

- Spring Boot backend with Java 21.
- PostgreSQL persistence and Flyway base schema.
- JWT login, refresh, current-user endpoint, and role-based access.
- Tenant-aware users and roles.
- Legacy roles currently in code: `ADMIN`, `FPO_MANAGER`, `FIELD_COORDINATOR`.
  Phase 1 target roles are `ADMIN`, `FPO_MANAGER`, and `FIELD_COORDINATOR`.
- Admin/FPO_MANAGER FIELD_COORDINATOR profile management.
- Role management APIs.
- Configurable workflow definitions.
- Activity creation and task timeline APIs.
- Evidence upload metadata, file storage, and review APIs.
- Audit event model and service.
- Report summary API.
- FPO dashboard summary API for member, land, plot, crop plan, and input demand
  metrics.
- FPO Excel export workbook with farmer, landholding, plot, crop history,
  seasonal crop plan, input demand summary, and farmer-wise demand sheets.
- PDF and Excel export foundation.
- Notification event/status foundation.
- Local and MinIO storage providers behind the same storage interface.
- Production profile validation for database, JWT, CORS, and storage.
- Standard API envelope and paginated response contract.

Frontend foundation:

- Expo React Native + TypeScript app.
- Backend-first login.
- Admin dashboard foundation.
- FIELD_COORDINATOR dashboard foundation.
- Admin FPO member management connected to backend.
- Admin workflow list/create/assign flows connected to backend.
- FIELD_COORDINATOR activity timeline connected to backend.
- Proof photo upload connected to backend evidence API.
- Admin evidence review screen.
- Admin roles tab.
- Admin notifications tab.
- Admin reports tab with export button.
- Admin FPO member management backed by `/api/v1/fpo/members`, including
  create, search, inline edit, and status changes.
- Admin crop planning backed by `/api/v1/fpo/crops`,
  `/api/v1/fpo/seasons`, `/api/v1/fpo/members/{memberId}/crop-history`, and
  `/api/v1/fpo/crop-plans`.
- Crop catalog, season setup, crop history, and seasonal crop plan UI.
- Admin input catalog, crop input rule, demand calculation, demand summary, and
  farmer-wise estimate UI backed by `/api/v1/fpo/inputs`,
  `/api/v1/fpo/input-rules`, and `/api/v1/fpo/demand-estimates`.
- Admin carbon operations tab with dummy carbon identity, soil score, activity
  verification, carbon potential, and dealer/lab directory records based on the
  client carbon app flow.
- Farmer carbon tab with dummy carbon identity, soil profile, activity scoring,
  advisory alerts, and nearby dealer/lab records.
- Admin advisory management UI backed by `/api/v1/fpo/advisories`, with local
  dummy fallback for prototype/demo mode.
- Frontend typecheck and lint passing.

Infrastructure and documentation:

- Docker Compose for PostgreSQL, MinIO, backend, and Expo web.
- Backend and frontend Dockerfiles.
- Local and production environment examples.
- GitHub CI workflow for frontend and backend checks.
- Security/dependency scan workflow.
- Deployment guide and production checklist.
- Architecture, database, QA, FPO_MANAGER, and component diagram docs.

### Remaining FPO MVP Boundaries

The platform now has both reusable concepts and first-class FPO records:

- `User`
- `Workflow`
- `Activity`
- `Task`
- `Evidence`
- `Report`
- `Notification`
- `AuditEvent`
- `FpoMemberProfile`
- `FarmLandholding`
- `FarmPlot`
- `CropCatalog`
- `CropSeason`
- `SeasonalCropPlan`
- `InputCatalog`
- `InputDemandEstimate`
- `FpoAdvisory`

The remaining risk is now implementation alignment against the locked Phase 1
decision register:

- Role and FPO isolation: `ADMIN`, `FPO_MANAGER`, `FIELD_COORDINATOR`.
- Approved farmer, land, soil, crop, advisory, and report fields.
- Confirmed-only input demand with 5% buffer, round-up, and `confirmed_at`.
- Exact three-sheet Excel workbook and optional date-range behavior.
- Advisory crop targeting and image attachments.
- Production operations: AWS Mumbai planning, backups, monitoring, and security
  patch ownership.

## Coverage Against Phase 1 Proposal

| Proposal Item                       | Current Status                 | Developer Note                                                                                                                  |
| ----------------------------------- | ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------- |
| Mobile app for farmers/coordinators | Future for farmers, partial for coordinators | Farmer mobile app is excluded from Phase 1; coordinators use the admin web dashboard on laptop/tablet.                          |
| Web/admin dashboard                 | Partial                        | Admin foundation and FPO dashboard summary API exist; align dashboard to locked Phase 1 fields and role scopes.                 |
| Backend APIs and database           | Strong foundation              | FPO schema, member, land/plot, crop, season, history, crop plan, input catalog, input rule, and demand estimate APIs now exist. |
| Farmer registration                 | Done for admin-created members | FIELD_COORDINATOR creation plus FPO member profile API exists and admin UI is connected.                                              |
| Mobile number authentication        | Future                         | OTP/SMS is explicitly excluded from Phase 1.                                                                                     |
| Farmer profile management           | Functional foundation done     | Align to full name, mobile, Aadhaar optional, village/taluka/district/state, gender, category, status, FPO, coordinator.        |
| Landholding capture                 | Done                           | Survey/khasra, acres, approved ownership, approved irrigation, schema checks, API validation, and admin UI controls are aligned. |
| GPS farm geo-tagging                | Done                           | GPS latitude/longitude are required for Phase 1 plot records; manual entry and browser/device GPS capture exist.                |
| Crop history                        | Done                           | Backend APIs and admin UI exist.                                                                                                |
| Current crop capture                | Partial                        | Seasonal crop plans now capture current plans; mobile farmer-side view and workflow linkage remain.                             |
| Seasonal crop planning              | Done                           | Crop, season, crop history, and crop plan APIs/UI exist.                                                                        |
| Acreage/input demand engine         | Functional foundation done     | Input catalog, crop input rules, calculation service, summary APIs, and admin UI exist.                                         |
| Basic notifications/advisory        | Backend ready                  | Advisory records and targeted read APIs exist; frontend UI remains.                                                             |
| Reports and Excel export            | FPO backend export foundation  | Refactor to approved `Farmer Register`, `Crop Plan Summary`, and `Input Demand` sheets.                                         |
| Role-based access control           | Partial                        | Replace legacy FPO_MANAGER assumptions with `FPO_MANAGER` and `FIELD_COORDINATOR` plus FPO scoping.                              |
| Testing and deployment support      | Strong foundation              | Add tests for new FPO modules.                                                                                                  |

Live coverage percentages are tracked only in
[Project Status And Gap Register](project-status-and-gap-register.md) to avoid
duplicate status drift.

## Recommended Commercial Phases

These are planning phases. Only Phase 0 and Phase 1 should be estimated firmly
before the client signs. Later phases need discovery.

| Phase   | Product Scope                                                                             |     Timeline | Indicative Cost |
| ------- | ----------------------------------------------------------------------------------------- | -----------: | --------------: |
| Phase 0 | Discovery, requirement freeze, data dictionary, report formats, workflow mapping          |       1 week |     Rs. 1L-1.5L |
| Phase 1 | FPO member data, land records, geo-tagging, crop planning, input demand MVP               |   8-10 weeks |   Rs. 13.5L-15L |
| Phase 2 | Input inventory, distribution, procurement, farmer ledger basics                          |   8-10 weeks |     Rs. 12L-18L |
| Phase 3 | Advisory engine, weather alerts, pest/disease advisory, WhatsApp/voice planning           |    6-8 weeks |     Rs. 10L-16L |
| Phase 4 | Market linkage, buyer portal, produce aggregation, quality grading, QR traceability       |  10-12 weeks |     Rs. 18L-28L |
| Phase 5 | NDVI/satellite, carbon tracking, sustainability reports, advanced analytics, integrations | 12-16+ weeks |    Rs. 25L-45L+ |

## Phase 0: Discovery And Scope Freeze

Goal:

- Convert the broad POC into a buildable Phase 1 specification.

Duration:

- 1 week.

Client inputs are now captured in
[Phase 1 Client Decision Register](phase1-client-decision-register.md). Do not
ask the client again for decisions already marked answered there.

Developer tasks:

- Create final data dictionary.
- Create final role matrix.
- Create final Phase 1 screen list.
- Create final API list.
- Create final report/export templates.
- Identify third-party services: SMS/OTP, map provider, weather provider.
- Use the approved decision that mobile/offline farmer workflows are Phase 2.
- Use the approved decision that `FIELD_COORDINATOR` is a separate Phase 1 role.
- Convert approved scope into GitHub issues or task board.

Acceptance criteria:

- Phase 1 scope is frozen.
- Report formats are approved.
- Computation formulas are approved.
- No Phase 2/3 modules are mixed into Phase 1.

## Phase 1 MVP: Developer Roadmap

Phase 1 should build the FPO operating foundation. The existing generic
workflow/activity/evidence platform should be reused, but FPO-specific records
should be explicit tables and APIs instead of being hidden in generic text
fields.

### Phase 1A: FPO Domain Model Foundation

Goal:

- Add FPO-specific data model while preserving reusable core modules.

Recommended backend package:

```text
com.activityplatform.backend.fpo
```

Recommended tables:

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

Developer tasks:

- Done: Add Flyway migration for FPO tables.
- Done: Add FPO member entity and repository.
- Done: Add FPO member API DTOs.
- Done: Keep tenant ownership on every durable table.
- Done: Link FPO member profile to existing `users.id`.
- Done: Add `created_at` and `updated_at` to mutable records.
- Done: Add indexes for tenant, village/location, crop, season, and farmer user id.
- Done: Add integration tests for FPO member tenant isolation, role access, and module-disabled behavior.
- Done: Add JPA entities/repositories for landholding and plot tables.
- Done: Add JPA entities/repositories for crop, season, crop history, and
  crop plan tables.
- Done: Add JPA entities/repositories for input demand tables.
- Done: Update diagrams for crop planning APIs and tables.

Suggested table responsibilities:

| Table                    | Purpose                                                                                   |
| ------------------------ | ----------------------------------------------------------------------------------------- |
| `fpo_member_profiles`    | Farmer/member identity, village, demographics, FPO member number, coordinator assignment. |
| `farm_landholdings`      | Farmer-level land ownership or operated area summary.                                     |
| `farm_plots`             | Individual plots with area, optional landholding link, GPS coordinates, soil, and status. |
| `crop_catalog`           | Tenant crop master data.                                                                  |
| `crop_seasons`           | Kharif/Rabi/Zaid/custom seasonal windows.                                                 |
| `farmer_crop_history`    | Historical crop per farmer/plot/season/year.                                              |
| `seasonal_crop_plans`    | Planned crop acreage for the current season.                                              |
| `input_catalog`          | Seeds, fertilizers, biologicals, micronutrients, mulching material.                       |
| `crop_input_rules`       | Formula rules by crop, season, input, unit, and acreage basis.                            |
| `input_demand_estimates` | Computed farmer/season/input demand snapshots.                                            |

Acceptance criteria:

- New schema migrates cleanly with Flyway.
- Existing auth/workflow/evidence APIs still pass tests.
- FPO entities are tenant-scoped.
- Farmer profile can be linked to one platform user.

### Phase 1B: Farmer Registration And Profile Management

Goal:

- Replace generic FIELD_COORDINATOR profile UI with real farmer/member profile
  management.

Backend tasks:

- Done: Add `GET /api/v1/fpo/members`.
- Done: Add `GET /api/v1/fpo/members/me`.
- Done: Add `POST /api/v1/fpo/members`.
- Done: Add `GET /api/v1/fpo/members/{memberId}`.
- Done: Add `PUT /api/v1/fpo/members/{memberId}`.
- Done: Add `PATCH /api/v1/fpo/members/{memberId}/status`.
- Done: Allow admin/FPO_MANAGER to create platform user + FPO member profile together.
- Done: Validate member number and mobile number uniqueness per tenant.
- Done: Record audit events: `FPO_MEMBER_CREATED`, `FPO_MEMBER_UPDATED`,
  `FPO_MEMBER_STATUS_CHANGED`.
- Done: Guard FPO member APIs with `MEMBER_DATA`.

Frontend tasks:

- Done: Rename FIELD_COORDINATOR UI labels to farmer/member where appropriate.
- Done: Add admin member list screen with search.
- Done: Add member create/edit form.
- Done: Show linked login username and status.
- Done: Add farm asset profile section to the member card.
- Pending: Add a dedicated member detail route if the admin workflow grows
  beyond inline management.
- Pending: Show linked roles after role-summary UX is confirmed.
- Pending: Show missing-profile warnings where a user exists without FPO details.

Suggested fields:

- Full name.
- Mobile number.
- Alternate mobile number.
- Village.
- Gram panchayat/block.
- District.
- FPO member number.
- Gender.
- Age or date of birth.
- Farmer category.
- Coordinator/FPO_MANAGER assignment.
- Status.

Tests:

- Admin can create member.
- FPO_MANAGER can create member if allowed by business rule.
- FIELD_COORDINATOR cannot create member.
- Duplicate mobile/member number returns validation error.
- Member list is tenant-scoped.

Acceptance criteria:

- Backend can add/list/update a farmer/member and persist it immediately.
- Admin can add/update/search a farmer/member from UI and see it immediately
  from backend.
- No demo farmers appear unless explicitly seeded.
- Member data survives app reload.

### Phase 1C: Landholding And Farm Plot Geo-Tagging

Goal:

- Capture land and plot data needed for crop planning and acreage computation.

Backend tasks:

- Done: Add landholding APIs:
  - `GET /api/v1/fpo/members/{memberId}/landholdings`
  - `POST /api/v1/fpo/members/{memberId}/landholdings`
  - `PUT /api/v1/fpo/landholdings/{landholdingId}`
  - `PATCH /api/v1/fpo/landholdings/{landholdingId}/status`
- Done: Add plot APIs:
  - `GET /api/v1/fpo/members/{memberId}/plots`
  - `POST /api/v1/fpo/members/{memberId}/plots`
  - `PUT /api/v1/fpo/plots/{plotId}`
  - `PATCH /api/v1/fpo/plots/{plotId}/status`
- Done: Store latitude/longitude with decimal precision.
- Done: Store area in acres.
- Done: Store survey number, cultivable area, ownership type, irrigation
  source, and soil type.
- Done: Record audit events for land/plot create, update, and status changes.

Frontend tasks:

- Done: Add landholding section inside the admin member detail/card.
- Done: Add plot list and plot form.
- Done: Capture current GPS coordinates where browser/device geolocation is
  available.
- Done: Allow manual coordinate entry for web/admin fallback.
- Done: Display total active plot acreage by member.
- Future: Map preview and polygon drawing are excluded from Phase 1; GPS point
  fields are enough.

Suggested plot fields:

- Plot name.
- Area in acres.
- Optional linked landholding.
- Latitude.
- Longitude.
- Soil type.
- Active/inactive.

Tests:

- Cannot create plot for another tenant's member.
- Invalid coordinates rejected.
- Cross-member landholding assignment is rejected.
- Plot update emits audit event.

Acceptance criteria:

- Admin/coordinator can add farm plots for a farmer.
- System can calculate total acreage from active plots.
- GPS values are stored and visible.
- Manual latitude/longitude entry still works when GPS permission is denied.

### Phase 1D: Crop History And Seasonal Crop Planning

Goal:

- Track what farmers grew before and what they plan to grow this season.

Backend tasks:

- Done: Add crop catalog APIs:
  - `GET /api/v1/fpo/crops`
  - `POST /api/v1/fpo/crops`
  - `PUT /api/v1/fpo/crops/{cropId}`
  - `PATCH /api/v1/fpo/crops/{cropId}/status`
- Done: Add season APIs:
  - `GET /api/v1/fpo/seasons`
  - `POST /api/v1/fpo/seasons`
  - `PUT /api/v1/fpo/seasons/{seasonId}`
  - `PATCH /api/v1/fpo/seasons/{seasonId}/status`
- Done: Add crop history APIs:
  - `GET /api/v1/fpo/members/{memberId}/crop-history`
  - `POST /api/v1/fpo/members/{memberId}/crop-history`
  - `PUT /api/v1/fpo/crop-history/{historyId}`
- Done: Add crop plan APIs:
  - `GET /api/v1/fpo/crop-plans`
  - `POST /api/v1/fpo/crop-plans`
  - `GET /api/v1/fpo/crop-plans/{planId}`
  - `PUT /api/v1/fpo/crop-plans/{planId}`
  - `PATCH /api/v1/fpo/crop-plans/{planId}/status`
- Done: Link crop plans to member, optional plot, crop, season, acreage, and planned dates.
- Pending: Optionally create an activity timeline from a crop plan using existing
  workflow definitions.

Frontend tasks:

- Done: Add crop catalog admin UI.
- Done: Add season setup UI.
- Done: Add member crop history section.
- Done: Add current season crop planning screen.
- Done: Show planned acreage, farmer count, village count, and plan count.
- Pending: Allow admin/FPO_MANAGER to start workflow/activity from a crop plan.

Suggested crop plan fields:

- Farmer/member.
- Plot.
- Crop.
- Season.
- Planned acreage.
- Expected sowing date.
- Expected harvest date.
- Plan status: `DRAFT`, `CONFIRMED`, `CANCELLED`, `COMPLETED`.

Tests:

- Plan acreage cannot exceed selected plot area unless explicitly allowed.
- Crop plan is tenant-scoped.
- Crop plan can generate input demand.
- Crop plan can optionally create activity timeline.

Acceptance criteria:

- FPO admin can see season-wise crop coverage.
- Each farmer can have crop history and current crop plan.
- Reports can summarize crop acreage by village/crop/season.

### Phase 1E: Input Demand Computation Engine

Goal:

- Convert crop plans and acreage into farmer-wise and FPO-level input demand.

Backend tasks:

- Done: Add input catalog APIs:
  - `GET /api/v1/fpo/inputs`
  - `POST /api/v1/fpo/inputs`
  - `PUT /api/v1/fpo/inputs/{inputId}`
  - `PATCH /api/v1/fpo/inputs/{inputId}/status`
- Done: Add crop input rule APIs:
  - `GET /api/v1/fpo/input-rules`
  - `POST /api/v1/fpo/input-rules`
  - `PUT /api/v1/fpo/input-rules/{ruleId}`
  - `PATCH /api/v1/fpo/input-rules/{ruleId}/status`
- Done: Add demand computation APIs:
  - `POST /api/v1/fpo/demand-estimates/run`
  - `GET /api/v1/fpo/demand-estimates`
  - `GET /api/v1/fpo/demand-estimates/summary`
- Done: Store calculation snapshots so reports are stable.
- Done: Support crop, season, village, input-rule, and status filters where
  relevant.
- Done: Record audit events: `FPO_INPUT_DEMAND_CALCULATED`.

Formula model:

```text
plannedAcreage * quantityPerAcre = requiredQuantity
```

Future formula model can support:

- Crop stage.
- Soil type.
- Irrigation type.
- District/village variation.
- Organic/conventional package.
- Rounding rules.
- Buffer percentage.

Frontend tasks:

- Done: Add input catalog UI.
- Done: Add crop input rule UI.
- Done: Add demand run button.
- Done: Add farmer-wise demand estimate list.
- Done: Add FPO summary by crop/input/village.
- Pending: Add FPO export action in the dashboard/report UI slice.

Tests:

- Done: Demand calculation is deterministic.
- Done: Missing input rules are counted and skipped clearly.
- Done: Summary totals are covered by controller integration test source.
- Done: Re-running calculation updates the existing crop-plan/input estimate.

Acceptance criteria:

- Done: Admin can generate consolidated demand from confirmed crop plans.
- Done: Admin can see crop-wise, input-wise, village-wise, and farmer-wise
  demand results.
- Done: Excel export contains the current Phase 1 backend workbook sheets.

### Phase 1F: Basic Advisory And Notification Flow

Goal:

- Provide basic notification/advisory capability without overbuilding AI,
  WhatsApp, or voice in Phase 1.

Backend tasks:

- Done: Add advisory records:
  - farmer/member or group target.
  - crop/season context.
  - message body.
  - channel.
  - status.
- Done: Add APIs:
  - `POST /api/v1/fpo/advisories`
  - `GET /api/v1/fpo/advisories`
  - `GET /api/v1/fpo/advisories/{advisoryId}`
  - `PATCH /api/v1/fpo/advisories/{advisoryId}/status`
- Done: For Phase 1, record notification/advisory status; do not promise actual SMS or
  WhatsApp delivery unless provider is separately approved.

Frontend tasks:

- Done: Add admin advisory list.
- Done: Add advisory create form.
- Done: Add farmer/FIELD_COORDINATOR advisory list inside the carbon tab.
- Done: Add status filters and crop/season/member/village targeting controls.

Tests:

- Pending: Automated test coverage is skipped for now per current development direction.
- Done: Admin/FPO_MANAGER can create advisory.
- Done: FIELD_COORDINATOR can view targeted published advisories only.
- Done: Advisory status changes are audited.

Acceptance criteria:

- Done: Admin can create a basic crop advisory/notification through backend APIs.
- Done: Farmer-facing advisory UI exists with backend/local fallback.
- Done: Delivery adapter remains replaceable.

### Phase 1G: FPO Reports And Dashboard

Goal:

- Convert generic reports into client-ready FPO reports.

Backend tasks:

- Done: Add FPO report summary API:
  - total farmers.
  - active farmers.
  - total land area.
  - geo-tagged plot count.
  - crop-wise acreage.
  - village-wise acreage.
  - season-wise crop plans.
  - input demand totals.
- Done: Add FPO Excel export:
  - Farmer master sheet.
  - Landholding sheet.
  - Plot geo-tagging sheet.
  - Crop history sheet.
  - Seasonal crop plan sheet.
  - Input demand summary sheet.
  - Farmer-wise demand sheet.
- Add optional PDF summary for management view.

Frontend tasks:

- Add FPO dashboard cards.
- Add crop/village/season filters.
- Add report export buttons.
- Add download/storage-key display.

Tests:

- Done: Summary endpoint service test covers aggregation.
- Done: Integration test source covers endpoint response and module guard.
- Done: Excel workbook contains all expected sheets.
- Pending: Totals should be checked against database rows during the broader
  API smoke/UAT catalog.

Acceptance criteria:

- Done: FPO admin can export a usable Excel workbook for management/government review.
- Dashboard gives basic planning visibility.

### Phase 1H: Mobile Authentication And UAT Hardening

Goal:

- Make the MVP usable by real farmers/coordinators.

Backend tasks:

- Decide OTP provider.
- Add OTP request/verify endpoints if included in signed scope:
  - `POST /api/v1/auth/otp/request`
  - `POST /api/v1/auth/otp/verify`
- Add rate limiting around OTP.
- Store OTP attempts securely and temporarily.
- Add production config for provider keys.
- Add audit events for OTP login.

Frontend tasks:

- Add mobile number login flow.
- Add OTP verify screen.
- Keep username/password login for admin if required.
- Improve validation and empty states for field users.

QA tasks:

- Prepare UAT test cases.
- Test Android/mobile browser behavior.
- Test slow network behavior.
- Test invalid/duplicate farmer data.
- Test export files.
- Test role access.

Acceptance criteria:

- Agreed login flow works for admin/coordinator/farmer.
- UAT blockers are resolved.
- Production deployment checklist is complete.

## Phase 2: Transaction And Operational Layer

Phase 2 should start only after Phase 1 data quality is validated.

Scope:

- Input inventory.
- Input purchase records.
- Input distribution to farmer.
- Farmer-wise ledger basics.
- Produce procurement records.
- Payment tracking.
- Stock movement.
- Basic logistics/dispatch records.

Developer tasks:

- Add inventory item and stock ledger tables.
- Add supplier and purchase tables.
- Add input distribution table linked to farmer/member and crop plan.
- Add produce procurement table linked to farmer/member and crop.
- Add payment record table.
- Add stock movement reports.
- Add audit events for transaction changes.
- Add role restrictions around financial actions.

Out of scope unless separately approved:

- Payment gateway.
- Accounting integration.
- GST/e-invoice integration.
- Full ERP.

## Phase 3: Advisory Decision Support

Scope:

- Crop-stage advisory engine.
- Weather API integration.
- Pest/disease alert templates.
- Soil health recommendations.
- Agronomist review workflow.
- WhatsApp/voice planning if approved.

How software helps:

- System knows crop, sowing date, crop stage, plot location, and weather.
- Rule engine identifies advisory triggers.
- Admin/agronomist can review or publish advisory.
- Farmer/coordinator receives in-app notification or configured delivery.

Human intervention:

- Required for advisory content approval.
- Required for crop issue diagnosis until AI/photo recognition is separately
  implemented and validated.
- Required for local language and practical recommendation quality.

Optional technical inputs:

- Weather API.
- Soil test data.
- Uploaded crop photos.
- Satellite/NDVI.
- IoT/weather station data.

## Phase 4: Market Linkage And Traceability

Scope:

- Produce aggregation.
- Lot creation.
- Quality grading.
- QR/barcode traceability.
- Buyer inquiries.
- Buyer portal.
- Traceability reports.

Developer tasks:

- Add harvest/produce lot model.
- Link lot to crop plan, farmer, plot, input/advisory/activity history.
- Add quality grade model.
- Add QR code generation.
- Add buyer-facing read-only traceability page.
- Add export-ready traceability report.

## Phase 5: Sustainability, Carbon, Satellite, And Advanced Analytics

Scope:

- NDVI/satellite monitoring.
- Sustainability practice tracking.
- Carbon practice records.
- Carbon/sustainability reporting.
- Forecasting and advanced analytics.
- External government/marketplace integrations.

Developer tasks:

- Select satellite/NDVI provider.
- Add geospatial plot boundary support if required.
- Add sustainability practice taxonomy.
- Add carbon activity records.
- Add analytics data marts or summary tables.
- Add scheduled jobs for data refresh.
- Add external API integration adapters.

## Recommended Implementation Rules

### Preserve Generic Core

Do:

- Keep `User`, `Workflow`, `Activity`, `Evidence`, `Report`, and `Audit`
  generic.
- Add FPO-specific tables in a separate FPO module.
- Use workflow/activity for field process tracking.
- Use FPO tables for farmer, land, crop, season, and input demand.

Avoid:

- Hardcoding crop names inside core modules.
- Storing farmer/plot/crop details only as generic strings.
- Mixing marketplace, carbon, and procurement into Phase 1.
- Promising AI/IoT/WhatsApp before provider and budget are approved.

### Human And Automation Boundary

For Phase 1:

- Software captures data, calculates acreage/demand, tracks status, and exports
  reports.
- Humans configure crops, formulas, report formats, and advisory messages.

For later phases:

- Weather/satellite/IoT can trigger alerts.
- Agronomists or client experts should still approve sensitive recommendations.
- AI should assist, not replace, expert validation until field accuracy is
  proven.

### Optional IoT Boundary

IoT is not required for Phase 1.

Design later integration through a provider-neutral pattern:

- `sensor_devices`
- `sensor_readings`
- `weather_station_readings`
- `external_data_sources`

For Phase 1, use manual entry, GPS capture, report upload, and optional weather
API placeholders.

## Immediate Next Developer Tasks

These are the next practical coding tasks if the project moves toward the FPO
MVP.

1. Done: Add `fpo` backend package and Flyway migration for
   member/land/plot/crop/input foundation.
2. Done: Add member profile APIs and integration tests.
3. Done: Convert admin FIELD_COORDINATOR UI into farmer/member management UI.
4. Done: Add landholding and plot APIs.
5. Done: Add landholding and plot UI with GPS coordinate capture.
6. Done: Add crop catalog and season setup APIs.
7. Done: Add crop history and seasonal crop plan APIs and admin UI.
8. Done: Add input catalog, crop input rules, and demand calculation service.
9. Done: Add FPO dashboard summary endpoint.
10. Done: Add FPO Excel export workbook sheets.
11. Done: Add basic advisory records using existing notification foundation.
12. Done: Add advisory UI with backend and local dummy fallback.
13. Next: Add carbon backend data dictionary/schema draft, or start OTP/mobile
    login only after provider and commercial scope are confirmed.
14. Update diagrams and QA guide after each schema/API slice.

## Suggested GitHub Issue Breakdown

Backend:

- `FPO-001`: Done: Flyway migration for FPO member, land, plot, crop, season, and input tables.
- `FPO-002`: Done: FPO member entity/repository/service/controller.
- `FPO-003`: Done: Landholding and plot entity/repository/service/controller.
- `FPO-004`: Done: Crop catalog and season APIs.
- `FPO-005`: Done: Crop history and seasonal crop plan APIs.
- `FPO-006`: Done: Input catalog and crop input rule APIs.
- `FPO-007`: Done: Input demand calculation service and summary endpoint.
- `FPO-008`: Done: FPO dashboard summary API and Excel export workbook service.
- `FPO-009`: Done: Advisory/notification API extension.
- `FPO-010`: OTP auth spike and provider abstraction.

Frontend:

- `FPO-FE-001`: Done: Farmer/member list and create/edit form.
- `FPO-FE-002`: Done: Member detail screen with profile sections.
- `FPO-FE-003`: Done: Landholding and plot forms.
- `FPO-FE-004`: Done: GPS capture and manual coordinate fallback.
- `FPO-FE-005`: Done: Crop catalog and season setup UI.
- `FPO-FE-006`: Done: Crop history and seasonal plan UI.
- `FPO-FE-007`: Done: Input catalog/rule management UI.
- `FPO-FE-008`: Done: Demand calculation summary UI.
- `FPO-FE-009`: Done: FPO dashboard and report export UI.
- `FPO-FE-010`: Mobile/OTP login UI if approved.
- `FPO-FE-011`: Done: Carbon app-flow dummy UI foundation.
- `FPO-FE-012`: Done: Advisory create/list/publish/archive UI.

QA/DevOps:

- `FPO-QA-001`: UAT test case catalog for Phase 1.
- `FPO-QA-002`: API smoke collection for FPO endpoints.
- `FPO-QA-003`: Excel export verification dataset.
- `FPO-QA-004`: Deployment checklist for client UAT environment.

Docs:

- `FPO-DOC-001`: Update architecture and database diagrams for FPO module.
- `FPO-DOC-002`: Add admin/farmer user guide.
- `FPO-DOC-003`: Add client UAT checklist.
- `FPO-DOC-004`: Add operations runbook.

## Phase 1 Definition Of Done

Phase 1 is complete only when:

- Admin can create and manage farmer/member records.
- Admin can add landholding and geo-tagged plots.
- Admin/coordinator can record crop history.
- Admin/coordinator can create current seasonal crop plans.
- System can compute acreage and input demand from approved formulas.
- Admin can view FPO-level dashboard summaries.
- Admin can export agreed Excel reports.
- Farmer/coordinator can see assigned profile/crop/advisory information.
- Role permissions are enforced.
- Audit events exist for important changes.
- Tests cover API/security/database behavior for new FPO modules.
- Deployment docs and UAT guide are updated.
