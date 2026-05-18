# Project Documentation

This folder contains the working documentation for the reusable
activity-compliance platform.

## Canonical Documents

Start here when looking for a specific answer:

| Need                                    | Source of truth                                                                                 |
| --------------------------------------- | ----------------------------------------------------------------------------------------------- |
| Current status, percentages, gaps       | [Project Status And Gap Register](project-status-and-gap-register.md)                            |
| Stop, clean, reset, fresh start         | [Clean Start Runbook](clean-start-runbook.md)                                                    |
| Day-to-day developer setup and rules    | [Developer Guide](developer-guide.md)                                                           |
| Test commands and QA strategy           | [QA Guide](qa-guide.md)                                                                         |
| Production deployment and security      | [Deployment Guide](deployment-guide.md), [Deployment Security](DEPLOYMENT_SECURITY.md)           |
| Platform architecture and module rules  | [Architecture Guide](architecture.md), [Modular Platform Strategy](modular-platform-strategy.md) |
| Canonical farmer identity foundation    | [Farmer Identity Foundation](farmer-identity-foundation.md)                                      |
| Roles, page visibility, farmer login    | [Roles And Supervisors](roles-and-supervisors.md)                                                |
| FPO locked Phase 1 scope and UAT        | [Phase 1 Client Decision Register](phase1-client-decision-register.md), [FPO Phase 1 UAT Guide](fpo-phase1-uat-guide.md) |
| Carbon client delivery sequencing       | [Carbon Client Delivery Plan](carbon-client-delivery-plan.md)                           |
| Carbon screen status and UAT            | [Carbon Flow Screen Status And Task Breakdown](carbon-flow-screen-status-task-breakdown.md), [Carbon UAT Guide](carbon-uat-guide.md) |
| Future hardening and standards backlog  | [Foundation Hardening Roadmap](foundation-hardening-roadmap.md)                                  |

- [Clean Start Runbook](clean-start-runbook.md): stop all local services, remove
  stale generated data, reset Docker volumes, restart from defaults, and verify
  the app from a fresh state.
- [Project Status And Gap Register](project-status-and-gap-register.md): current
  module completion percentages, go-live confidence, testing audit, and future
  gaps.
- [Phase 1 Client Decision Register](phase1-client-decision-register.md):
  locked FPO Phase 1 scope, answered client decisions, implementation defaults,
  and items that should not be re-asked.
- [FPO Phase 1 Data Dictionary](fpo-phase1-data-dictionary.md): approved
  farmer, land, soil, crop, input demand, advisory, and report fields.
- [FPO Phase 1 UAT Guide](fpo-phase1-uat-guide.md): client-approved UAT
  scenarios, test data, entry criteria, and exit criteria.
- [Developer Guide](developer-guide.md): setup, local commands, ports, backend
  and frontend standards, API areas, testing, and implementation workflow.
- [QA Guide](qa-guide.md): automated checks, local smoke tests, API smoke tests,
  current coverage, and regression checklist.
- [Deployment Guide](deployment-guide.md): Docker stack, production runtime,
  required environment variables, and release checklist.
- [Deployment Security](DEPLOYMENT_SECURITY.md): production security hardening,
  dependency scanning, deployment safeguards, and maintenance checklist.
- [Foundation Hardening Roadmap](foundation-hardening-roadmap.md): UI/backend
  standards, TDD/QA strategy, performance/load testing, rollback/file
  consistency, observability, backup/restore, and future hardening tasks.
- [Farmer Identity Foundation](farmer-identity-foundation.md): canonical
  `users -> farmer_profiles -> module extension records` rule, schema,
  `FarmerService` contract, and safe migration sequence.

## Architecture And Product Planning

- [Architecture Guide](architecture.md): system architecture, module boundaries,
  diagrams, data model, request flow, and reuse rules.
- [Component And Data Model Diagrams](component-and-data-model-diagrams.md):
  component architecture, backend module map, and PostgreSQL class/table
  diagrams.
- [Modular Platform Strategy](modular-platform-strategy.md): modular monolith
  decision, tenant module subscriptions, delivery models, and future
  microservice split criteria.
- [Use Case Guide](use-cases.md): actors, use-case diagram, main flows, role
  permissions, and agriculture scenario.
- [FPO MVP Roadmap](fpo-mvp-roadmap.md): internal phase-wise roadmap,
  current-vs-pending coverage, and developer task breakdown for the FPO product.
- [FPO Developer Task List](fpo-developer-task-list.md): checklist-style
  execution tasks with status, dependencies, tests, and acceptance criteria.
- [Carbon Accounting App Flow Base Plan](carbon-app-flow-base-plan.md): client
  app-flow translation, dummy-data assumptions, non-blocked build scope, and
  provider/client decisions for the regenerative agriculture carbon platform.
- [Carbon Client Delivery Plan](carbon-client-delivery-plan.md): project-manager
  delivery sequencing for the latest client Excel, including P0/P1/P2 bands,
  sprint order, client decisions, test commands, and go-live checklist.
- [Carbon Flow Screen Status And Task Breakdown](carbon-flow-screen-status-task-breakdown.md):
  screen-by-screen status and epic task breakdown for the latest client Excel.
- [Carbon App Flow Task Roadmap](carbon-app-flow-task-roadmap.md): phase-wise
  earlier Carbon execution plan, task IDs, module-toggle rules, and
  demo/production readiness definitions.
- [Carbon Data Dictionary](carbon-data-dictionary.md): durable Carbon profile,
  farm, soil, and activity-category fields.
- [Carbon UAT Guide](carbon-uat-guide.md): Carbon-first package UAT scenarios,
  entry criteria, and exit criteria.
- [API Standards](api-standards.md): REST shape, response envelope, errors, and
  current endpoint conventions.
- [Database Notes](database.md): schema and database design notes.
- [Security Assessment](SECURITY_ASSESSMENT.md): current security review notes.
- [Client Admin Workflow Guide](client-admin-workflow-guide.md): admin workflow
  notes for the agriculture client.
- [Roles And Supervisors](roles-and-supervisors.md): role meanings,
  field-coordinator setup, farmer login, permissions, and daily usage.

## Documentation Status

These documents describe the current development foundation. They are suitable
for internal development, deployment preparation, and planning. The repository
also includes `.env.example` for local Docker defaults and
`.env.production.example` for production environment planning.

To prevent duplicate status drift:

- Keep completion percentages only in
  [Project Status And Gap Register](project-status-and-gap-register.md).
- Keep reset/restart commands only in
  [Clean Start Runbook](clean-start-runbook.md).
- Keep client Phase 1 decisions only in
  [Phase 1 Client Decision Register](phase1-client-decision-register.md).
- Keep FPO roadmap sequencing in [FPO MVP Roadmap](fpo-mvp-roadmap.md) and FPO
  execution checklists in [FPO Developer Task List](fpo-developer-task-list.md).
- Keep latest client Excel delivery sequencing in
  [Carbon Client Delivery Plan](carbon-client-delivery-plan.md).
- Keep screen-by-screen Excel status in
  [Carbon Flow Screen Status And Task Breakdown](carbon-flow-screen-status-task-breakdown.md).
- Keep older Carbon app-flow task IDs in
  [Carbon App Flow Task Roadmap](carbon-app-flow-task-roadmap.md) until they are
  consolidated or archived.
- Keep Carbon field definitions in [Carbon Data Dictionary](carbon-data-dictionary.md)
  and Carbon acceptance scenarios in [Carbon UAT Guide](carbon-uat-guide.md).
- Keep cross-cutting hardening tasks and standards in
  [Foundation Hardening Roadmap](foundation-hardening-roadmap.md) so they do not
  get duplicated inside product-specific roadmaps.
- Keep farmer identity rules and migration sequencing in
  [Farmer Identity Foundation](farmer-identity-foundation.md).

## Documentation Cleanup Backlog

The docs are still in a flat folder to avoid breaking links during active
development, but the target structure is:

```text
docs/
├── 00-index/          # README and document map
├── 10-platform/       # architecture, modular strategy, roles, standards
├── 20-fpo/            # FPO decisions, data dictionary, UAT, roadmap
├── 30-carbon/         # Carbon app flow, data dictionary, UAT, roadmap
├── 40-operations/     # clean start, deployment, security, backup/restore
├── 50-qa/             # QA guide, test catalog, automation strategy
└── 90-archive/        # old or superseded notes kept only for history
```

Before moving files, update all relative links in one commit and keep this
README as the entry point. Candidate consolidation items:

- Merge or archive older admin notes if [ADMIN_GUIDE](ADMIN_GUIDE.md) and
  [Client Admin Workflow Guide](client-admin-workflow-guide.md) duplicate the
  current [Roles And Supervisors](roles-and-supervisors.md) and product UAT
  guides.
- Keep status percentages out of product roadmaps; link back to
  [Project Status And Gap Register](project-status-and-gap-register.md).
- Keep repeated architecture rules out of individual task docs; link to
  [Modular Platform Strategy](modular-platform-strategy.md) and
  [Developer Guide](developer-guide.md).
- Keep hardening tasks only in
  [Foundation Hardening Roadmap](foundation-hardening-roadmap.md), with product
  roadmaps linking to task IDs instead of copying acceptance criteria.

Before client production handoff, create a final client-facing documentation set:

- Final operations runbook.
- User/admin manual.
- Full QA/UAT test case catalog.
- Backup and recovery guide.
