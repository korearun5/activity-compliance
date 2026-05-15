# Project Status And Gap Register

Last audited: 2026-05-15

This is the single source for current completion estimates, go-live confidence,
and known gaps. Percentages are engineering confidence estimates based on the
current repository state, not client acceptance or a commercial commitment.

## Go-Live Snapshot

| Area                         | Completion | Confidence | Notes |
| ---------------------------- | ---------: | ---------- | ----- |
| Local developer setup        |        90% | High       | Clean-start path, Docker stack, env examples, CI commands, and default ports are documented. |
| Core backend platform        |        88% | High       | Auth, tenant-aware data, workflows, activity, evidence, audit, reports, storage, and common API envelope exist. |
| Frontend platform shell      |        82% | Medium     | Backend-first admin/FPO/coordinator shell, farmer workflow routing, module-driven carbon hiding, and centralized role-to-tab/action visibility exist; frontend still lacks automated UI tests. |
| Auth, users, and roles       |        93% | High       | JWT, role APIs, local seeds, frontend routing, staff-login creation, farmer-profile login creation, role-aware UI controls, and tests now use `ADMIN`, `FPO_MANAGER`, `FIELD_COORDINATOR`, and `FARMER`. |
| Workflow and activity engine |        82% | High       | Configurable definitions, activity start, task status, and tests exist; client-specific workflow templates still need UAT. |
| Evidence and storage         |        80% | Medium     | Local and MinIO adapters exist; MinIO integration test coverage is still missing. |
| Reporting and exports        |        84% | High       | Generic summary/export and the FPO approved three-sheet workbook exist; filters, header/footer branding, and focused tests are wired. |
| Module subscription platform |        82% | Medium     | Backend guards, frontend module visibility, and Phase 2 carbon screen hiding behind `SUSTAINABILITY` exist; packaging/handover process remains pending. |
| FPO member management        |        90% | High       | Approved farmer fields, farmer username/password login, coordinator assignment, tenant/FPO scoping, farmer-profile UI wording, validation, and focused tests are aligned. |
| FPO land and plot records    |        82% | High       | Survey/khasra, acres, approved ownership/irrigation values, required GPS latitude/longitude, schema checks, API validation, UI controls, and tests are aligned; polygon maps remain Phase 2. |
| FPO soil profiles            |        82% | High       | Phase 1 SOC, pH, N, P, K, optional report link/metadata, backend API, admin entry UI, JUnit, and Testcontainers coverage are in place; real S3 upload wiring remains grouped with advisory/storage work. |
| FPO crop planning            |        85% | High       | Catalog, seasons, crop history, seasonal plans, crop year labels, optional expected yield, confirmation timestamp, UI, and tests are aligned; farmer mobile views are Phase 2. |
| FPO input demand             |        84% | High       | Catalog, input rules, confirmed-only calculation, 5% buffer, round-up, summaries, UI, migration, and tests are aligned; approved workbook output remains in report work. |
| FPO advisory                 |        82% | High       | Category, all-members/crop targeting, multiple image links, in-app-only channel validation, UI previews, and focused Testcontainers coverage are aligned. |
| Carbon app-flow prototype    |        35% | Low        | Frontend dummy screens/data exist but are hidden behind the disabled `SUSTAINABILITY` module for Phase 1; durable schema, methodology, providers, and exports are pending. |
| QA automation                |        71% | Medium     | JUnit, Spring tests, Testcontainers PostgreSQL, Phase 1 UAT backend smoke coverage, CI, lint, and typecheck exist; UI/E2E tests and coverage gates are missing. |
| Production operations        |        60% | Medium     | Production config validation, security scan, env template, and deployment docs exist; backups, monitoring, alerting, and runbooks need target-environment details. |

Overall readiness:

- Developer/demo environment: about 85%.
- FPO MVP technical foundation: about 83%.
- FPO Phase 1 go-live readiness before UAT: about 89-90% after client scope lock; implementation gaps remain.
- Full client POC vision including OTP, maps, satellite, AI, carbon, marketplace, and payments: about 20-25%.

## Testing And Quality Audit

Present now:

- JUnit/Jupiter through Spring Boot test starters.
- Backend unit tests under `backend/src/test/java`.
- Backend API integration tests named `*IT.java`.
- PostgreSQL Testcontainers through `spring-boot-testcontainers`,
  `testcontainers-junit-jupiter`, and `testcontainers-postgresql`.
- Testcontainers PostgreSQL image pinned to `postgres:17-alpine`.
- Maven integration profile: `.\mvnw.cmd -Pintegration-test verify`.
- Frontend checks: `npm run typecheck` and `npm run lint`.
- CI jobs for frontend lint/typecheck, backend unit tests, backend integration
  tests, Docker Compose config, npm audit, and OWASP Dependency Check.

Gaps to schedule:

- Add frontend component or screen tests.
- Add browser/E2E smoke tests for login, admin FPO flows, FIELD_COORDINATOR flows, and FARMER workflow access.
- Add MinIO-backed storage integration tests.
- Add Testcontainers coverage for object storage if production storage remains
  MinIO/S3-compatible.
- Add Jacoco minimum coverage thresholds if the team wants CI to enforce a
  numeric floor.
- Configure Mockito as an explicit test Java agent before future JDKs disable
  dynamic agent loading.
- Confirm the Spring `open-in-view` runtime warning in integration logs and
  keep it disabled for API deployments.
- Expand automated coverage for the approved UAT scenarios beyond the backend
  smoke path.
- Add remaining role matrix tests for `ADMIN`, `FPO_MANAGER`,
  `FIELD_COORDINATOR`, and `FARMER` permissions where UI coverage is still
  manual.

## Documentation Cleanup Rules

To avoid duplicate or conflicting information:

- Use this file for completion percentages, current status, and gap tracking.
- Use [Clean Start Runbook](clean-start-runbook.md) for stop, clean, reset, and
  restart commands.
- Use [Developer Guide](developer-guide.md) for day-to-day setup and coding
  standards.
- Use [QA Guide](qa-guide.md) for test commands and smoke checks.
- Use [Deployment Guide](deployment-guide.md) and
  [Deployment Security](DEPLOYMENT_SECURITY.md) for production planning.
- Keep roadmap documents focused on product sequencing, not live status tables.

## Go-Live Blockers

| Gap | Status | Owner | Why It Matters |
| --- | ------ | ----- | -------------- |
| Final FPO data dictionary | Done | Product/Tech Lead | Locked in [Phase 1 Client Decision Register](phase1-client-decision-register.md) and [FPO Phase 1 Data Dictionary](fpo-phase1-data-dictionary.md). |
| Approved input formulas | Done | Product/Tech Lead | Fixed per-acre values, confirmed plans only, 5% buffer, and round-up are approved. |
| Approved report templates | Done | Product/Tech Lead | Excel-only, three sheets, approved columns, filters, branding text, and footer are documented. |
| UAT scenario catalog | Done | QA/Product | [FPO Phase 1 UAT Guide](fpo-phase1-uat-guide.md) contains pilot data and acceptance scenarios. |
| Role foundation alignment | Done | Backend/Frontend/QA | Active app code now uses `ADMIN`, `FPO_MANAGER`, `FIELD_COORDINATOR`, and `FARMER`; staff creation is separate from farmer profile/login creation; frontend page/action visibility is centralized in `roleAccess.ts`; focused role/user/member tests pass. |
| Farmer profile field alignment | Done | Backend/Frontend/QA | Taluka/state, optional Aadhaar, approved gender/category values, Indian mobile normalization, and suspended status are implemented. |
| Land/GPS field alignment | Done | Backend/Frontend/QA | Survey/khasra, acres, approved ownership/irrigation values, and required GPS point capture are implemented. |
| FPO ownership/scoped access alignment | Done | Backend/Frontend/QA | `FARMER` username/password login is restored for Phase 1, member profiles link to farmer users, coordinators are scoped to assigned members, and focused role isolation tests pass. |
| Soil profile entry | Done | Backend/Frontend/QA | SOC, pH, N, P, K, optional report link/metadata, no carbon calculation, admin UI entry, and focused tests are implemented. |
| Crop plan Phase 1 alignment | Done | Backend/Frontend/QA | Crop plans now store `crop_year`, optional expected yield, and `confirmed_at`; UI/API contracts and focused unit/integration tests are aligned. |
| Input demand alignment | Done | Backend/Frontend/QA | Demand now uses `CONFIRMED` crop plans only, stores total demand, 5% buffer, and rounded final demand, and exposes those values in API/UI summaries with focused unit and Testcontainers coverage. |
| Report workbook alignment | Done | Backend/Frontend/QA | FPO export now emits exactly `Farmer Register`, `Crop Plan Summary`, and `Input Demand` with approved columns and focused workbook/controller tests. |
| Report filters and branding | Done | Backend/Frontend/QA | Village/crop/season/coordinator/date filters are sent from the report UI and applied with sheet-specific date semantics; Excel header/footer branding is emitted. |
| Advisory image and crop targeting alignment | Done | Backend/Frontend/QA | All-members/crop targeting, advisory categories, in-app-only channel validation, multiple image link storage records, UI previews, and focused API tests are implemented. |
| Production secrets and hosting | Pending | DevOps/Client | Required for secure deployment outside local/dev. |
| Backups and restore drill | Pending | DevOps | Production readiness requires verified recovery, not only a backup command. |
| Monitoring and alerting | Pending | DevOps | Needed for API, DB, storage, and background failure visibility. |
| Frontend automated tests | Pending | Frontend/QA | Reduces regression risk for admin and farmer screens. |
| MinIO integration tests | Pending | Backend/QA | Confirms production-like object storage behavior. |
| Production image pinning | Pending | DevOps | Local MinIO can use an overrideable image; production should pin approved image tags. |
| OTP/SMS provider | Future | Client/Provider | Explicitly excluded from Phase 1. |
| Map/boundary provider | Future | Client/Provider | GPS point capture only is approved for Phase 1. |
| Carbon methodology | Future | Client/Product | Carbon calculation is explicitly excluded from Phase 1. |

## Next Cleanup Tasks

1. Continue expanding `FPO-ALIGN-010` with frontend smoke/E2E checks and any
   remaining manual-only UAT scenarios.
2. Rehearse the clean-start runbook on a fresh machine or clean Windows profile.
3. Finalize the client-facing operations manual after production hosting is
   chosen.
4. Convert the UAT guide into automated smoke/integration coverage where useful.
5. Decide whether to enforce Jacoco coverage thresholds in CI.
6. Add frontend test tooling before the UI grows further.
7. Add storage integration coverage for MinIO/S3-compatible flows.
