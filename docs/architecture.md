# Architecture Notes

## Direction

This project should be built as a reusable activity-compliance platform. The agriculture client is the first domain, not the core model.

Core language should stay generic:

- `User` for farmer, field worker, inspector, supervisor, or admin.
- `Workflow` for crop lifecycle, inspection checklist, audit process, or field program.
- `Activity` for one execution of a workflow.
- `Task` for one required stage inside a workflow.
- `Evidence` for proof photos, notes, documents, and timestamps.
- `Report` for PDF, Excel, and dashboard summaries.

Client/domain language can appear in UI copy, seed data, report labels, and configuration records.

## Frontend Structure

- `src/core`: reusable platform model, API contracts, storage helpers, workflow helpers, logging, and errors.
- `src/auth`: temporary demo auth. Backend JWT integration should replace internals without changing screen behavior.
- `src/data`: local demo stores and agriculture seed data. These should shrink once Spring Boot APIs are available.
- `src/screens`: current Expo screens.
- `src/ui`: reusable presentation components.

## Backend Target Structure

Use a modular Spring Boot monolith first:

- `auth`: login, refresh, JWT, password handling, current user.
- `user`: user profile, role assignment, tenant membership.
- `workflow`: configurable workflow definitions and stage/task rules.
- `activity`: activity instances, task status, timeline.
- `evidence`: upload metadata, local storage adapter now, MinIO adapter later.
- `audit`: append-only activity/user/system audit events.
- `reporting`: PDF/Excel generation and report history.
- `notification`: notification templates and delivery status.
- `common`: exceptions, response wrappers, validation, logging, security helpers.

Keep transaction boundaries at service methods. Controllers should validate/request-map only, services should own business rules, repositories should own persistence.

## Reuse Rules

- Do not hardcode agriculture workflow stages in code.
- Store workflow definitions and task order in database tables.
- Keep storage behind a Java interface, with local and MinIO implementations.
- Keep report generation data-source driven, not farmer-specific.
- Make tenant identifiers optional in early local/demo work, but include them in backend schema from day one.

