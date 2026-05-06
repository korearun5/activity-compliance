# Database Guide

The backend uses PostgreSQL with Flyway migrations. The schema is reusable: agriculture is one configuration of the workflow model, not a separate hardcoded database design.

For visual class/table diagrams, see
[Component And Data Model Diagrams](component-and-data-model-diagrams.md).

## Tables In Use Now

- `tenants`: client/organization boundary.
- `users`: login users and participant profile basics.
- `roles`: tenant-scoped role records.
- `user_roles`: user-to-role assignments.
- `workflow_definitions`: admin-defined process templates, such as a crop cycle.
- `workflow_tasks`: ordered task/stage list for each workflow definition.
- `activities`: one participant executing one workflow.
- `activity_tasks`: runtime task status for one activity.
- `evidence`: uploaded proof file metadata, notes, review status, and storage key.
- `audit_events`: append-only compliance trail for user/activity/evidence/report events.
- `report_exports`: generated PDF/Excel export history.
- `notification_events`: queued notification/status messages.

## Data Ownership

- Backend owns users, roles, workflows, activities, activity tasks, evidence, reports, and audit history.
- Frontend may keep session tokens, active form state, selected local photo before upload, and temporary prototype cache only.
- Crop/task definitions must be configured through workflow records. They should not be hardcoded in the UI.

## Workflow Pattern

1. Admin creates a row in `workflow_definitions`.
2. Admin adds ordered rows in `workflow_tasks`.
3. Participant starts an `activity` from the active workflow.
4. Backend creates `activity_tasks` from the configured task list.
5. Participant uploads proof to `evidence`.
6. Backend updates task/activity progress.
7. Admin reviews proof and later generates reports from these records.
