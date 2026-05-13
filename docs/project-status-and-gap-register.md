# Project Status And Gap Register

Last audited: 2026-05-13

This is the single source for current completion estimates, go-live confidence,
and known gaps. Percentages are engineering confidence estimates based on the
current repository state, not client acceptance or a commercial commitment.

## Go-Live Snapshot

| Area                         | Completion | Confidence | Notes |
| ---------------------------- | ---------: | ---------- | ----- |
| Local developer setup        |        90% | High       | Clean-start path, Docker stack, env examples, CI commands, and default ports are documented. |
| Core backend platform        |        88% | High       | Auth, tenant-aware data, workflows, activity, evidence, audit, reports, storage, and common API envelope exist. |
| Frontend platform shell      |        78% | Medium     | Backend-first admin and participant shell exists; frontend still lacks automated UI tests. |
| Auth, users, and roles       |        85% | High       | JWT, role checks, admin/supervisor user management, and role APIs exist; OTP/mobile login is blocked. |
| Workflow and activity engine |        82% | High       | Configurable definitions, activity start, task status, and tests exist; client-specific workflow templates still need UAT. |
| Evidence and storage         |        80% | Medium     | Local and MinIO adapters exist; MinIO integration test coverage is still missing. |
| Reporting and exports        |        78% | Medium     | Generic summary/export and FPO Excel export exist; final client report templates need approval. |
| Module subscription platform |        80% | Medium     | Backend guards and frontend module visibility exist; packaging/handover process remains pending. |
| FPO member management        |        82% | High       | Backend APIs, admin UI, status handling, and integration tests exist; final field list still needs client freeze. |
| FPO land and plot records    |        72% | Medium     | APIs, UI, manual GPS fields, and tests exist; map preview/boundary drawing is not included. |
| FPO crop planning            |        80% | High       | Catalog, seasons, crop history, seasonal plans, UI, and tests exist; farmer-side/mobile views remain limited. |
| FPO input demand             |        78% | Medium     | Catalog, input rules, calculation, summaries, UI, and tests exist; final formulas must be approved. |
| FPO advisory                 |        68% | Medium     | Advisory backend and admin UI exist; push/SMS/WhatsApp delivery is provider-dependent. |
| Carbon app-flow prototype    |        35% | Low        | Frontend dummy screens/data exist; durable schema, methodology, providers, and exports are pending. |
| QA automation                |        68% | Medium     | JUnit, Spring tests, Testcontainers PostgreSQL, CI, lint, and typecheck exist; UI/E2E tests and coverage gates are missing. |
| Production operations        |        60% | Medium     | Production config validation, security scan, env template, and deployment docs exist; backups, monitoring, alerting, and runbooks need target-environment details. |

Overall readiness:

- Developer/demo environment: about 85%.
- FPO MVP technical foundation: about 75-80%.
- FPO Phase 1 go-live readiness before UAT: about 65-70%.
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
- Add browser/E2E smoke tests for login, admin FPO flows, and participant flows.
- Add MinIO-backed storage integration tests.
- Add Testcontainers coverage for object storage if production storage remains
  MinIO/S3-compatible.
- Add Jacoco minimum coverage thresholds if the team wants CI to enforce a
  numeric floor.
- Configure Mockito as an explicit test Java agent before future JDKs disable
  dynamic agent loading.
- Confirm the Spring `open-in-view` runtime warning in integration logs and
  keep it disabled for API deployments.
- Add client UAT test catalog with approved FPO scenarios and sample data.
- Add role matrix tests for all admin, supervisor, participant, and future field
  coordinator permissions.

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
| Final FPO data dictionary | Pending | Product/Client | Prevents field churn in member, land, crop, input, and report forms. |
| Approved input formulas | Pending | Product/Client | Demand estimates cannot be trusted until units, rounding, and buffers are approved. |
| Approved report templates | Pending | Product/Client | Excel/PDF export layout and columns need final sign-off. |
| UAT scenario catalog | Pending | QA/Product | Go-live confidence needs repeatable client acceptance scripts. |
| Production secrets and hosting | Pending | DevOps/Client | Required for secure deployment outside local/dev. |
| Backups and restore drill | Pending | DevOps | Production readiness requires verified recovery, not only a backup command. |
| Monitoring and alerting | Pending | DevOps | Needed for API, DB, storage, and background failure visibility. |
| Frontend automated tests | Pending | Frontend/QA | Reduces regression risk for admin and farmer screens. |
| MinIO integration tests | Pending | Backend/QA | Confirms production-like object storage behavior. |
| Production image pinning | Pending | DevOps | Local MinIO can use an overrideable image; production should pin approved image tags. |
| OTP/SMS provider | Blocked | Client/Provider | Mobile login cannot be built safely without provider details. |
| Map/boundary provider | Blocked | Client/Provider | Plot boundary drawing and map previews need provider and accuracy decisions. |
| Carbon methodology | Blocked | Client/Product | Carbon estimates must remain prototype-only until methodology is approved. |

## Next Cleanup Tasks

1. Rehearse the clean-start runbook on a fresh machine or clean Windows profile.
2. Finalize the client-facing operations manual after production hosting is
   chosen.
3. Add UAT test cases for each FPO module and link them from the QA guide.
4. Decide whether to enforce Jacoco coverage thresholds in CI.
5. Add frontend test tooling before the UI grows further.
6. Add storage integration coverage for MinIO/S3-compatible flows.
