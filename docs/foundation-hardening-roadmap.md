# Foundation Hardening Roadmap

Last updated: 2026-05-19

This document captures the standards and future hardening tasks discussed after
the Carbon enrollment UI cleanup. It is intentionally not a Phase 1 feature
scope document. Use it to align existing code with the stronger platform
standard over time without forgetting reliability, security, performance, QA,
and reusable-product concerns.

## Decision Summary

Current decision:

```text
Keep a modular monolith now. Harden module boundaries, tests, operations, and
reusable UI/API patterns before considering microservices.
```

Why:

- The product/domain is still evolving.
- Phase 1 and the Carbon app flow need fast iteration.
- Cross-module flows benefit from shared transactions and simple debugging.
- Microservices would add deployment, network, monitoring, tracing, testing,
  data-consistency, and DevOps overhead before the domain is stable.
- BPMN/workflow engines should be considered later only if workflows become
  highly dynamic, approval-heavy, or client-authored.

## What 100% Means In Engineering Terms

Do not promise literal 100% speed, reliability, automation, or fault tolerance.
For production engineering, convert those expectations into measurable targets:

- Availability target, for example `99.5%` or `99.9%`.
- API latency targets, for example p95 and p99 for critical endpoints.
- Recovery Point Objective (RPO): maximum acceptable data loss window.
- Recovery Time Objective (RTO): maximum acceptable restoration time.
- Error-rate threshold and alerting rules.
- Test coverage gates for critical modules.
- Backup retention and restore-drill cadence.

## Current Standards To Follow

### UI/UX Standards

- Default to view-first, action-second screens.
- Lists, summaries, and selected-record details should be visible before forms.
- Large add/edit forms should use a modal, drawer, wizard, or dedicated detail
  page instead of filling the main screen.
- Use modals for focused short-to-medium forms where the user can save or
  cancel and return to the same context.
- Use a dedicated page or wizard for very long workflows, multi-step approvals,
  or flows with many attachments.
- Complex forms should show common fields first and hide less-used fields under
  advanced sections.
- Every screen must have clear loading, empty, error, disabled-module, and
  access-denied states.
- Role/module visibility must be centralized; do not scatter role checks across
  many components.
- Farmer identity fields must be shared. Carbon/FPO/future module screens must
  compose the shared farmer identity component instead of copying name, mobile,
  location, gender, category, username, and password fields.
- Shared workflow screens must consume a platform participant contract, not an
  FPO-only or Carbon-only record type.
- Shared UI patterns belong under `src/ui` or a module-local UI folder if they
  are module-specific.

### Frontend Architecture Standards

- Shared platform concerns stay in `src/core`, `src/auth`, `src/navigation`, and
  `src/ui`.
- Product-specific screens, API adapters, stores, and navigation should live
  under `src/modules/<module>` whenever practical.
- Existing FPO code can move gradually when touched; do not perform risky
  folder-only churn during client-critical work.
- `EXPO_PUBLIC_ENABLED_CLIENT_MODULES` is a packaging/UX switch only, not source
  license protection.
- Backend enabled-module checks remain the authorization boundary.
- Platform roles, user login, farmer identity, and activity participant
  selection are cross-module foundations. Product modules extend them with
  module-specific records, but do not redefine or duplicate them.
- Use Playwright for future web E2E/browser smoke tests. Consider Detox or an
  Expo-compatible approach later for native mobile E2E.

### Backend Standards

- Keep controllers thin: HTTP mapping, validation, and response envelope only.
- Keep business rules in services.
- Use explicit transaction boundaries:
  - `@Transactional(readOnly = true)` for queries.
  - `@Transactional` for state changes.
- Keep tenant and module checks near the use case boundary.
- Emit audit events for critical state changes.
- Do not put business decisions in repositories.
- Use Flyway for every schema change.
- Add indexes for tenant-scoped list/filter/report paths before production
  load grows.
- Keep reusable platform code in common/core-like backend packages and
  product-specific rules under the owning product package.
- Shared farmer profile validation belongs in the backend farmer package and is
  reused by FPO and Carbon services.
- Canonical farmer identity belongs in `farmer_profiles`; modules extend that
  record instead of owning separate farmer identities. See
  [Farmer Identity Foundation](farmer-identity-foundation.md).

### Testing Standards

- Use TDD or test-first development for business rules, calculations, access
  rules, and bug fixes where the expected behavior is clear.
- Add unit tests for pure validation/calculation/rule logic.
- Add integration tests for controller, security, module guard, transaction,
  and database behavior.
- Add UI component/screen tests or browser smoke tests before the UI grows much
  further.
- Do not chase arbitrary 100% test count; protect critical use cases and
  regressions first, then add coverage gates once the foundation is stable.

### Transaction, Rollback, And File Handling Standards

- Database writes should be atomic inside service-level transactions.
- DB and object storage are not naturally one atomic transaction. For file
  flows, use one of these patterns:
  - Store metadata as `PENDING`, upload object, then mark `READY`.
  - Save object first, then write metadata, with cleanup if metadata fails.
  - Use a retry/outbox or cleanup job for orphaned objects.
- Do not keep long file/report generation work inside a large database
  transaction.
- Workflow state changes should be auditable and should prefer forward
  compensating actions over destructive rollback of business history.

### Performance And Reliability Standards

- Add load tests only for realistic use cases and agreed targets.
- Start with critical paths: login, list Carbon profiles, add profile, add plot,
  add soil profile, upload evidence, export report.
- Use k6 or Gatling for backend/API load tests. Use Playwright for user-flow
  browser smoke; do not use browser E2E as the main load tool.
- Monitor JVM memory, connection pool saturation, database slow queries, API
  latency, error rate, storage errors, and disk usage.
- Review deadlock risk around concurrent status changes, report exports, and
  multi-record updates before high-volume production usage.
- Define production resource limits and JVM memory settings before deployment.

### Backup, Restore, And Failover Standards

- A backup is not complete until a restore drill has passed.
- Production readiness requires:
  - Scheduled PostgreSQL backup.
  - Object storage backup/replication/lifecycle plan.
  - Restore runbook.
  - Tested restore into a separate environment.
  - RPO/RTO agreed with the client.
  - Monitoring for backup failures.
- Primary database failure handling depends on the selected hosting platform
  and must be finalized during production architecture.

## Farmer Identity Foundation Sprint

Use this sprint to fix the root identity issue before adding more farmer-facing
features.

| ID                        | Status      | Area                 | Task                                                                                         | Acceptance                                                                                         |
| ------------------------- | ----------- | -------------------- | -------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| FOUNDATION-FARMER-001     | Done        | Backend schema/core  | Add canonical `farmer_profiles`, nullable FPO/Carbon links, and the `FarmerService` contract. | New schema migrates safely; farmer-only users can receive canonical profiles; existing reads work.  |
| FOUNDATION-FARMER-002     | Done        | FPO alignment        | Link FPO member profiles to canonical farmer profiles.                                        | New FPO member create/update/status paths use `FarmerService`; old FPO reads continue during migration. |
| FOUNDATION-FARMER-003     | Done        | Carbon alignment     | Link Carbon profiles to canonical farmer profiles.                                            | New Carbon farmer create/update paths use `FarmerService`; existing Carbon reads remain compatible. |
| FOUNDATION-FARMER-004     | Done        | Backend service      | Standardize farmer identity access behind the `FarmerService` contract.                       | FPO and Carbon runtime paths use one canonical service for farmer create, ensure, update, and participant lookup. |
| FOUNDATION-MIGRATION-001  | Done        | Data migration       | Backfill canonical farmer profiles from FPO and Carbon data with duplicate-review safeguards.  | Exact `user_id`, selected FPO member, and deterministic unique-mobile links are audited; ambiguous records are flagged, not silently merged.  |
| FOUNDATION-PARTICIPANT-001 | Done       | Workflow foundation  | Resolve workflow participants through canonical farmer users.                                  | Platform participant endpoint reads active canonical farmers; activity assignment rejects non-canonical farmer users; assigned farmers can list their activities. |
| FOUNDATION-UI-001         | Done        | Farmer UI            | Replace module-gated farmer activity access with one common farmer dashboard.                 | Farmer activity tabs are common activity-compliance tabs, Carbon-only farmers still see assigned activities, and backend farmer sessions no longer fall back to stale module-local activity records. |
| FOUNDATION-QA-001         | Pending     | QA                   | Add regression coverage for Carbon-only, FPO-only, and combined module configurations.         | Automated tests protect farmer create, login, assignment, and activity visibility across packages.  |

Details live in [Farmer Identity Foundation](farmer-identity-foundation.md).

## Existing Code Alignment Tasks

These are future hardening tasks. Do not treat them as active sprint work unless
the sprint explicitly selects them.

| ID         | Status  | Area                 | Task                                                                                                                                                   | Acceptance                                                                                                                       |
| ---------- | ------- | -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------- |
| HARDEN-001 | Pending | UI standards         | Audit existing screens for large inline forms and convert the worst offenders to modal, drawer, wizard, or detail-page patterns.                       | Main workflows follow view-first/action-second UX; add/edit has clear save/cancel; no screen is dominated by rarely used inputs. |
| HARDEN-002 | In progress | UI components        | Extract reusable modal, action header, empty state, error state, advanced section, and confirmation components.                                        | Shared farmer identity fields now exist; remaining modal/empty/error/advanced primitives still need extraction.                  |
| HARDEN-003 | Pending | Frontend modules     | Move FPO-owned screen/data/API boundaries into `src/modules/fpo` when next touched.                                                                    | FPO and Carbon remain independently loadable; shared code stays in core/ui.                                                      |
| HARDEN-004 | In progress | Role/module UX       | Standardize role and module visibility states across all screens.                                                                                      | Central role/module registry and shared workflow participant contract exist; disabled/unauthorized states still need browser coverage. |
| HARDEN-005 | In progress | Backend boundaries   | Review backend package boundaries and split reusable platform logic from FPO/Carbon product-specific logic where drift exists.                         | Shared farmer validation now lives in the backend farmer package; continue extracting common platform rules when duplication appears. |
| HARDEN-006 | Pending | Transactions         | Audit service transaction boundaries, workflow state changes, and rollback behavior.                                                                   | State-changing use cases have explicit transaction strategy and audit behavior.                                                  |
| HARDEN-007 | Pending | File consistency     | Define and implement DB/object-storage consistency pattern for evidence, soil reports, advisory images, and exports.                                   | Failed upload/metadata paths leave no silent data loss and can be retried or cleaned up.                                         |
| HARDEN-008 | Pending | Database performance | Review indexes for tenant-scoped filters, reports, Carbon profile/farm/soil lists, FPO member/crop/demand queries, and audit tables.                   | Query plans for common list/report paths are acceptable under expected data volume.                                              |
| HARDEN-009 | Pending | Performance tests    | Add API load-test scripts for login, Carbon enrollment, evidence upload metadata, and report export trigger paths.                                     | Load tests run locally or in staging and report p95/p99 latency, error rate, and throughput.                                     |
| HARDEN-010 | Pending | E2E QA               | Add Playwright web smoke tests for login, module visibility, Carbon enrollment modal flows, FPO manager flow, field coordinator flow, and farmer flow. | Core browser journeys run in CI or release rehearsal without manual clicking.                                                    |
| HARDEN-011 | Pending | Coverage gates       | Decide and implement coverage thresholds after the current test suite stabilizes.                                                                      | CI prevents major untested regressions without blocking reasonable refactors.                                                    |
| HARDEN-012 | Pending | Observability        | Define production logs, metrics, traces, dashboards, and alert rules.                                                                                  | Operators can see API errors, latency, DB health, storage health, and backup failures.                                           |
| HARDEN-013 | Pending | Backup/restore       | Create and rehearse production backup and restore runbooks.                                                                                            | Restore succeeds into a separate environment within agreed RTO/RPO.                                                              |
| HARDEN-014 | Pending | Security             | Add frontend E2E access-control checks and schedule external penetration/security review before production handoff.                                    | Disabled/restricted screens cannot be accessed through UI or direct API calls.                                                   |
| HARDEN-015 | Pending | Source packaging     | Prepare source distribution process for Carbon-only, FPO-only, and full-platform deliveries.                                                           | Client handover packages include licensed modules plus shared core only.                                                         |
| HARDEN-016 | Pending | Production resources | Define JVM memory, container resources, DB connection pool settings, and OOM/deadlock review checklist.                                                | Production deploy has resource limits and diagnostics for memory/connection failures.                                            |
| HARDEN-017 | Pending | Workflow engine      | Reassess whether current workflow tables are enough or whether BPMN/worker orchestration is justified.                                                 | BPMN/microservice split happens only if real workflow complexity requires it.                                                    |

## Suggested Hardening Sprint Order

1. UI standards and modal/component extraction: `HARDEN-001`, `HARDEN-002`.
2. Module boundary cleanup: `HARDEN-003`, `HARDEN-004`, `HARDEN-005`.
3. Transaction/file consistency: `HARDEN-006`, `HARDEN-007`.
4. Browser/E2E and load-test foundation: `HARDEN-009`, `HARDEN-010`.
5. Production operations: `HARDEN-012`, `HARDEN-013`, `HARDEN-016`.
6. Source handover packaging and commercial protection: `HARDEN-015`.

## Related Source-Of-Truth Docs

- Completion percentages and gap status:
  [Project Status And Gap Register](project-status-and-gap-register.md).
- Test commands and QA strategy: [QA Guide](qa-guide.md).
- Reusable architecture rules: [Architecture Guide](architecture.md).
- Modular monolith and handover strategy:
  [Modular Platform Strategy](modular-platform-strategy.md).
- Carbon execution task IDs:
  [Carbon App Flow Task Roadmap](carbon-app-flow-task-roadmap.md).
- FPO execution task IDs: [FPO Developer Task List](fpo-developer-task-list.md).
