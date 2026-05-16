# Project Documentation

This folder contains the working documentation for the reusable
activity-compliance platform.

## Canonical Documents

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
- [Carbon App Flow Task Roadmap](carbon-app-flow-task-roadmap.md): phase-wise
  Carbon execution plan, task IDs, module-toggle rules, and demo/production
  readiness definitions.
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
- Keep Carbon app-flow sequencing and task IDs in
  [Carbon App Flow Task Roadmap](carbon-app-flow-task-roadmap.md).
- Keep Carbon field definitions in [Carbon Data Dictionary](carbon-data-dictionary.md)
  and Carbon acceptance scenarios in [Carbon UAT Guide](carbon-uat-guide.md).
- Keep cross-cutting hardening tasks and standards in
  [Foundation Hardening Roadmap](foundation-hardening-roadmap.md) so they do not
  get duplicated inside product-specific roadmaps.

Before client production handoff, create a final client-facing documentation set:

- Final operations runbook.
- User/admin manual.
- Full QA/UAT test case catalog.
- Backup and recovery guide.
