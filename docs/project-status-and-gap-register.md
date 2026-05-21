# Project Status And Gap Register

Last audited: 2026-05-21

This is the single source for current completion estimates, go-live confidence,
and known gaps. Percentages are engineering confidence estimates based on the
current repository state, not client acceptance or a commercial commitment.

## Go-Live Snapshot

| Area                         | Completion | Confidence | Notes                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ---------------------------- | ---------: | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Local developer setup        |        95% | High       | Clean-start path, Docker stack, env examples, CI commands, and default ports are documented and rehearsed locally on 2026-05-15.                                                                                                                                                                                                                                                                                                                             |
| Core backend platform        |        88% | High       | Auth, tenant-aware data, workflows, activity, evidence, audit, reports, storage, and common API envelope exist.                                                                                                                                                                                                                                                                                                                                              |
| Frontend platform shell      |        92% | High       | Carbon-first client shell, FPO/coordinator foundation, farmer workflow routing, FPO UI toggle, centralized role-to-tab/action visibility, shared farmer identity fields, and backend-backed activity participant registry exist; automated UI tests remain go-live hardening.                                                                                                                                                                                |
| Auth, users, and roles       |        93% | High       | JWT, role APIs, local seeds, frontend routing, staff-login creation, farmer-profile login creation, role-aware UI controls, and tests now use `ADMIN`, `FPO_MANAGER`, `FIELD_COORDINATOR`, and `FARMER`.                                                                                                                                                                                                                                                     |
| Workflow and activity engine |        82% | High       | Configurable definitions, activity start, task status, and tests exist; client-specific workflow templates still need UAT.                                                                                                                                                                                                                                                                                                                                   |
| Evidence and storage         |        80% | Medium     | Local and MinIO adapters exist; MinIO integration test coverage is still missing.                                                                                                                                                                                                                                                                                                                                                                            |
| Reporting and exports        |       100% | High       | Phase 1 workbook emits approved sheets/columns with filters, header/footer branding, and focused tests.                                                                                                                                                                                                                                                                                                                                                      |
| Module subscription platform |        94% | Medium     | Backend guards, central frontend module visibility registry, Carbon/FPO client package toggles, canonical participant registry, Carbon module extraction, and module visibility smoke coverage exist; handover packaging remains pending.                                                                                                                                                                                                                    |
| FPO member management        |       100% | High       | Approved farmer fields, farmer username/password login, coordinator assignment, tenant/FPO scoping, farmer-profile UI wording, validation, and focused tests are aligned.                                                                                                                                                                                                                                                                                    |
| FPO land and plot records    |       100% | High       | Survey/khasra, acres, approved ownership/irrigation values, required GPS latitude/longitude, schema checks, API validation, UI controls, and tests are aligned; polygon maps remain Phase 2.                                                                                                                                                                                                                                                                 |
| FPO soil profiles            |       100% | High       | Phase 1 SOC, pH, N, P, K, optional report link/metadata, backend API, admin entry UI, JUnit, and Testcontainers coverage are in place without carbon calculation.                                                                                                                                                                                                                                                                                            |
| FPO crop planning            |       100% | High       | Catalog, seasons, crop history, seasonal plans, crop year labels, optional expected yield, confirmation timestamp, UI, and tests are aligned; farmer mobile views are Phase 2.                                                                                                                                                                                                                                                                               |
| FPO input demand             |       100% | High       | Catalog, input rules, confirmed-only calculation, 5% buffer, round-up, summaries, UI, report output, migration, and tests are aligned.                                                                                                                                                                                                                                                                                                                       |
| FPO advisory                 |       100% | High       | Category, all-members/crop targeting, multiple image links/storage metadata, in-app-only channel validation, UI previews, and focused Testcontainers coverage are aligned.                                                                                                                                                                                                                                                                                   |
| Carbon app-flow prototype    |        83% | Medium     | Carbon screens/data are enabled by default, dashboard widgets match the App Flow shell, data dictionary/UAT docs exist, durable profile/farm/soil/activity schema is in place, backend profile/plot/soil/activity APIs are Testcontainers-verified, frontend profile/plot/soil/activity forms are wired, direct soil PDF/image upload uses the storage adapter, Carbon workflow evidence review is wired, and admin farmer/activity/soil verification queues exist; methodology, provider integrations, reports/exports, and UAT seed polish remain. |
| QA automation                |        90% | High       | JUnit, Spring tests, Testcontainers PostgreSQL, Phase 1 UAT backend smoke and role matrix coverage, CI, lint, typecheck, and local integration verification are green; UI/E2E tests and coverage gates remain hardening.                                                                                                                                                                                                                                     |
| Production operations        |        60% | Medium     | Production config validation, security scan, env template, and deployment docs exist; backups, monitoring, alerting, and runbooks need target-environment details.                                                                                                                                                                                                                                                                                           |
| Foundation hardening         |        74% | Medium     | Standards are captured in the [Foundation Hardening Roadmap](foundation-hardening-roadmap.md); canonical `farmer_profiles`, `FarmerService`, FPO/Carbon farmer-profile linkage, shared farmer validation, canonical activity participant registry, common farmer activity dashboard access, assigned farmer activity reads, and audited historical backfill now exist, while broader UI primitives, module relocation, transactions, file consistency, load/E2E tests, observability, backup/restore, and source packaging remain future sprint work. |

Overall readiness:

- Phase 1 feature development: 100% for the locked client scope.
- Developer/demo environment: about 95%.
- FPO MVP technical foundation: about 95%.
- FPO Phase 1 go-live readiness before client UAT: about 94-95%; development
  gaps are closed, while client sign-off and production operations remain.
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
- Frontend checks: `npm run typecheck`, `npm run lint`, and
  `npm run test:module-visibility`.
- CI jobs for frontend lint/typecheck, backend unit tests, backend integration
  tests, Docker Compose config, npm audit, and OWASP Dependency Check.
- Local verification on 2026-05-15:
  - `npm run typecheck` passed.
  - `npm run lint` passed.
  - `npm audit --audit-level=moderate` passed with zero vulnerabilities after
    pinning the transitive PostCSS advisory through npm overrides.
  - `.\mvnw.cmd test` passed with 40 unit tests.
  - `.\mvnw.cmd -Pintegration-test verify` passed with 87 integration tests.
  - `.\mvnw.cmd "-Dtest=FpoPhase1UatSmokeIT" test` passed after the role
    smoke fixture was corrected.
  - `docker compose config` passed.
  - `docker compose up -d --build backend frontend` built and started the full
    local stack.
  - Backend health returned `UP` on `http://localhost:8080/actuator/health`.
  - Frontend returned HTTP `200` on `http://localhost:19006`.
  - Local seed admin login succeeded and enabled-module API returned Phase 1
    modules.
  - Docker PostgreSQL accepted `activity_app/activity_app` on the default
    compose path, confirming the earlier password failure is not present after
    the clean dependency startup.
- Local verification on 2026-05-16 during Carbon API work:
  - `.\mvnw.cmd -DskipTests compile` passed.
  - `.\mvnw.cmd -DskipTests test-compile` passed.
  - `docker info --format '{{.ServerVersion}}'` returned `28.3.2`.
  - `.\mvnw.cmd -Dtest=CarbonProfileControllerIT test` passed with 3 tests and
    Flyway migrated through `V9__carbon_profile_access_scope.sql`.
  - `npm run typecheck` passed after frontend Carbon profile wiring.
  - `npm run lint` passed after frontend Carbon profile wiring.
  - `npm run test:module-visibility` passed after frontend Carbon profile
    wiring.
- Local verification on 2026-05-18 during shared farmer/module foundation work:
  - `npm run typecheck` passed after workflow participants were moved from FPO
    member records to the shared activity participant contract.
  - `npm run lint` passed after shared farmer identity extraction.
  - `npm run test:module-visibility` passed after preserving the central role
    and module visibility registry.
  - `.\mvnw.cmd -DskipTests compile` passed after backend farmer validation was
    moved to the shared farmer package.
- Local verification on 2026-05-18 during `FOUNDATION-FARMER-001`:
  - `.\mvnw.cmd -DskipTests compile` passed after adding canonical
    `farmer_profiles`, repository, and service classes.
  - `.\mvnw.cmd "-Dtest=FarmerProfileServiceTest" test` passed with
    Testcontainers PostgreSQL and Flyway migrated through `V13`.
  - `.\mvnw.cmd "-Dtest=FpoMemberControllerIT,CarbonProfileControllerIT,ActivityControllerIT" test`
    passed with 18 integration tests, confirming current FPO, Carbon, and
    Activity reads still work with the nullable future linkage columns.
- Local verification on 2026-05-18 during `FOUNDATION-FARMER-002`:
  - `.\mvnw.cmd -DskipTests compile` passed after wiring FPO members to
    canonical farmer profiles.
  - `.\mvnw.cmd "-Dtest=FpoMemberControllerIT" test` passed with Flyway
    migrated through `V14`, proving new FPO members link to canonical farmer
    profiles and updates/status changes keep them aligned.
  - `.\mvnw.cmd "-Dtest=FarmerProfileServiceTest" test` passed after the FPO
    link guard migration.
- Local verification on 2026-05-18 during `FOUNDATION-FARMER-003`:
  - `.\mvnw.cmd -DskipTests compile` passed after wiring Carbon farmer profiles
    to canonical farmer profiles.
  - `.\mvnw.cmd "-Dtest=CarbonProfileControllerIT" test` passed, proving
    existing farmer users and newly created Carbon farmer logins are linked to
    canonical farmer profiles while Carbon plot/soil/activity flows still pass.
- Local verification on 2026-05-19 during `FOUNDATION-MIGRATION-001`:
  - `mvn "-Dtest=FarmerProfileBackfillMigrationTest" test` passed with
    Testcontainers PostgreSQL after Flyway migrated through `V15`, proving FPO
    and Carbon historical rows can be linked or flagged with audit records.
  - `mvn test` passed after tightening the backfill to create/link canonical
    farmer profiles only for farmer-only users.
  - `mvn "-Dtest=FpoMemberControllerIT,CarbonProfileControllerIT,ActivityControllerIT,FarmerProfileServiceTest,FarmerProfileBackfillMigrationTest" test`
    passed with 22 focused backend integration/service tests.
- Local verification on 2026-05-19 during `FOUNDATION-PARTICIPANT-001`:
  - `mvn "-Dtest=ActivityControllerIT,FarmerParticipantControllerIT,FarmerProfileServiceTest" test`
    passed with 14 focused backend integration/service tests.
  - `npm run typecheck` and `npm run lint` passed after wiring the frontend
    workflow participant picker to the platform participant endpoint.
  - `mvn test` passed with 44 backend unit/service tests.
  - `npm run test:module-visibility` passed after the participant registry
    frontend changes.
- Local verification on 2026-05-21 during `CARBON-CLIENT-011`:
  - `npm run typecheck` passed through `npm.cmd`.
  - `npm run lint` passed through `npm.cmd`.
  - `npm run test:module-visibility` passed through `npm.cmd`.
  - `mvn -DskipTests compile` passed after adding Carbon soil verification APIs.
  - `mvn -Dtest=CarbonProfileControllerIT test` passed after Flyway migrated
    through `V21`, proving Carbon soil pending/verify endpoints and module
    disabled checks.

Gaps to schedule:

- Add frontend component or screen tests as go-live hardening.
- Add browser/E2E smoke tests for login, admin FPO flows, FIELD_COORDINATOR flows, and FARMER workflow access as go-live hardening.
- Add MinIO-backed storage integration tests.
- Add Testcontainers coverage for object storage if production storage remains
  MinIO/S3-compatible.
- Add Jacoco minimum coverage thresholds if the team wants CI to enforce a
  numeric floor.
- Configure Mockito as an explicit test Java agent before future JDKs disable
  dynamic agent loading.
- Confirm the Spring `open-in-view` runtime warning in integration logs and
  keep it disabled for API deployments.
- Expand frontend/browser automation for the approved UAT scenarios after the
  current backend smoke path.
- Add remaining role matrix tests for `ADMIN`, `FPO_MANAGER`,
  `FIELD_COORDINATOR`, and `FARMER` permissions where UI coverage is still
  manual.

## Documentation Cleanup Rules

To avoid duplicate or conflicting information:

- Use [Documentation README](README.md) as the document map and cleanup backlog.
- Use this file for completion percentages, current status, and gap tracking.
- Use [Clean Start Runbook](clean-start-runbook.md) for stop, clean, reset, and
  restart commands.
- Use [Developer Guide](developer-guide.md) for day-to-day setup and coding
  standards.
- Use [QA Guide](qa-guide.md) for test commands and smoke checks.
- Use [Deployment Guide](deployment-guide.md) and
  [Deployment Security](DEPLOYMENT_SECURITY.md) for production planning.
- Keep roadmap documents focused on product sequencing, not live status tables.
- Keep Carbon app-flow task sequencing in
  [Carbon App Flow Task Roadmap](carbon-app-flow-task-roadmap.md).
- Keep cross-cutting standards and hardening task IDs in
  [Foundation Hardening Roadmap](foundation-hardening-roadmap.md).

## Go-Live Blockers

| Gap                                         | Status      | Owner               | Why It Matters                                                                                                                                                                                                                                                     |
| ------------------------------------------- | ----------- | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Final FPO data dictionary                   | Done        | Product/Tech Lead   | Locked in [Phase 1 Client Decision Register](phase1-client-decision-register.md) and [FPO Phase 1 Data Dictionary](fpo-phase1-data-dictionary.md).                                                                                                                 |
| Approved input formulas                     | Done        | Product/Tech Lead   | Fixed per-acre values, confirmed plans only, 5% buffer, and round-up are approved.                                                                                                                                                                                 |
| Approved report templates                   | Done        | Product/Tech Lead   | Excel-only, three sheets, approved columns, filters, branding text, and footer are documented.                                                                                                                                                                     |
| UAT scenario catalog                        | Done        | QA/Product          | [FPO Phase 1 UAT Guide](fpo-phase1-uat-guide.md) contains pilot data and acceptance scenarios.                                                                                                                                                                     |
| Role foundation alignment                   | Done        | Backend/Frontend/QA | Active app code now uses `ADMIN`, `FPO_MANAGER`, `FIELD_COORDINATOR`, and `FARMER`; staff creation is separate from farmer profile/login creation; frontend page/action visibility is centralized in `roleAccess.ts`; focused role/user/member tests pass.         |
| Farmer profile field alignment              | Done        | Backend/Frontend/QA | Taluka/state, optional Aadhaar, approved gender/category values, Indian mobile normalization, and suspended status are implemented.                                                                                                                                |
| Land/GPS field alignment                    | Done        | Backend/Frontend/QA | Survey/khasra, acres, approved ownership/irrigation values, and required GPS point capture are implemented.                                                                                                                                                        |
| FPO ownership/scoped access alignment       | Done        | Backend/Frontend/QA | `FARMER` username/password login is restored for Phase 1, member profiles link to farmer users, coordinators are scoped to assigned members, and focused role isolation tests pass.                                                                                |
| Soil profile entry                          | Done        | Backend/Frontend/QA | SOC, pH, N, P, K, optional report link/metadata, no carbon calculation, admin UI entry, and focused tests are implemented.                                                                                                                                         |
| Crop plan Phase 1 alignment                 | Done        | Backend/Frontend/QA | Crop plans now store `crop_year`, optional expected yield, and `confirmed_at`; UI/API contracts and focused unit/integration tests are aligned.                                                                                                                    |
| Input demand alignment                      | Done        | Backend/Frontend/QA | Demand now uses `CONFIRMED` crop plans only, stores total demand, 5% buffer, and rounded final demand, and exposes those values in API/UI summaries with focused unit and Testcontainers coverage.                                                                 |
| Report workbook alignment                   | Done        | Backend/Frontend/QA | FPO export now emits exactly `Farmer Register`, `Crop Plan Summary`, and `Input Demand` with approved columns and focused workbook/controller tests.                                                                                                               |
| Report filters and branding                 | Done        | Backend/Frontend/QA | Village/crop/season/coordinator/date filters are sent from the report UI and applied with sheet-specific date semantics; Excel header/footer branding is emitted.                                                                                                  |
| Advisory image and crop targeting alignment | Done        | Backend/Frontend/QA | All-members/crop targeting, advisory categories, in-app-only channel validation, multiple image link storage records, UI previews, and focused API tests are implemented.                                                                                          |
| Local clean-start and default ports         | Done        | Dev/QA              | Docker Desktop is reachable, compose config is valid, default ports are aligned, PostgreSQL and MinIO start healthy, and PostgreSQL credentials were verified on 2026-05-15.                                                                                       |
| Production secrets and hosting              | Pending     | DevOps/Client       | Required for secure deployment outside local/dev.                                                                                                                                                                                                                  |
| Backups and restore drill                   | Pending     | DevOps              | Production readiness requires verified recovery, not only a backup command.                                                                                                                                                                                        |
| Monitoring and alerting                     | Pending     | DevOps              | Needed for API, DB, storage, and background failure visibility.                                                                                                                                                                                                    |
| Frontend automated tests                    | Recommended | Frontend/QA         | Reduces regression risk for admin and farmer screens; not a Phase 1 feature blocker.                                                                                                                                                                               |
| MinIO integration tests                     | Recommended | Backend/QA          | Confirms production-like object storage behavior; local storage adapter and validation are already implemented.                                                                                                                                                    |
| Production image pinning                    | Pending     | DevOps              | Local MinIO can use an overrideable image; production should pin approved image tags.                                                                                                                                                                              |
| Source handover packaging                   | Pending     | Product/Tech Lead   | If client receives source, prepare a licensed distribution branch/package; frontend flags alone do not protect unlicensed FPO or future modules.                                                                                                                   |
| Carbon package tenant setup                 | Pending     | DevOps/Product      | Carbon-first frontend packaging also requires the tenant `SUSTAINABILITY` backend module to be enabled; do not rely on frontend package config alone.                                                                                                              |
| Carbon API implementation                   | Done        | Backend/QA          | Backend APIs for Carbon profiles, farmer login creation/linking, farm plots, soil profile metadata, soil report upload, activity categories, and activity records are implemented with role/module scoping and focused Testcontainers coverage.                    |
| Shared farmer and participant foundation    | Done        | Frontend/Backend/QA | Canonical `farmer_profiles`, `FarmerService`, shared validation, canonical participant endpoint, new FPO/Carbon farmer-profile linkage, audited historical backfill, common farmer dashboard access, and assigned farmer activity reads exist; `FOUNDATION-QA-001` remains for broader package-mode regression automation. |
| Carbon profile frontend wiring              | Done        | Frontend/QA         | Carbon admin/FPO/field-coordinator screens can list/create/update farmer enrollments with shared farmer identity/login fields, plots, soil metadata, and activity records; farmer Carbon screen reads the linked backend profile, plots, soil data, and activity data; admin verification queues now cover bank details, documents, Carbon activity evidence, and Carbon soil records. |
| Carbon existing-account picker              | Pending     | Frontend/Backend    | Carbon enrollment can store `userId`/`fpoMemberProfileId`, but the staff UI still needs a friendly picker for existing farmer users/FPO members before client demo polish.                                                                                         |
| Foundation hardening backlog                | Pending     | Tech Lead/QA/DevOps | [Foundation Hardening Roadmap](foundation-hardening-roadmap.md) captures future UI standards, backend transaction/file consistency, E2E/load tests, production reliability, source packaging, and reusable-code alignment tasks.                                   |
| OTP/SMS provider                            | Future      | Client/Provider     | Explicitly excluded from Phase 1.                                                                                                                                                                                                                                  |
| Map/boundary provider                       | Future      | Client/Provider     | GPS point capture only is approved for Phase 1.                                                                                                                                                                                                                    |
| Carbon methodology                          | Future      | Client/Product      | Carbon calculation is explicitly excluded from Phase 1.                                                                                                                                                                                                            |

## Next Cleanup Tasks

1. Rehearse the clean-start runbook on a fresh machine or clean Windows profile
   outside this current developer machine.
2. Finalize the client-facing operations manual after production hosting is
   chosen.
3. Decide whether to enforce Jacoco coverage thresholds in CI.
4. Add frontend test tooling before the UI grows further.
5. Add storage integration coverage for MinIO/S3-compatible flows.
6. Pick the first Foundation Hardening sprint slice from
   [Foundation Hardening Roadmap](foundation-hardening-roadmap.md) after the
   current Carbon/FPO client priorities are stable.
