# Client Admin Workflow Guide

This guide is for a client admin who needs to add their own crop lifecycle and task list. The app should not ship with dummy crops; each client configures real workflows for their process.

## Before You Start

- Backend must be running.
- Admin must have a valid username and password configured by the environment or user-management flow.
- Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

## 1. Login As Admin

Use:

```http
POST /api/v1/auth/login
```

Request body:

```json
{
  "tenantCode": "default",
  "username": "<admin-username>",
  "password": "<admin-password>"
}
```

Copy the `accessToken` from the response. In Swagger UI, click **Authorize** and enter:

```text
Bearer <accessToken>
```

## 2. Add A Crop Workflow

In the app, admins and FPO_MANAGERs can use the Admin dashboard `Workflows` tab
to create workflow definitions and task timelines. The same action is available
through the API:

Use:

```http
POST /api/v1/workflows
```

Request body:

```json
{
  "code": "client-crop-code",
  "name": "Client Crop Name",
  "domainKey": "agriculture",
  "durationDays": 90,
  "version": 1,
  "status": "ACTIVE",
  "tasks": [
    {
      "code": "task-1",
      "title": "First required task",
      "sequenceNumber": 10,
      "offsetDays": 0,
      "evidenceRequired": true
    },
    {
      "code": "task-2",
      "title": "Second required task",
      "sequenceNumber": 20,
      "offsetDays": 14,
      "evidenceRequired": true
    }
  ]
}
```

Replace the names, task titles, duration, and offsets with the client's real process.

## 3. Update A Workflow Safely

- If no FIELD_COORDINATOR has started activities from that workflow, use `PUT /api/v1/workflows/{workflowId}`.
- If FIELD_COORDINATORs have already started activities, create a new workflow version instead of changing task order.
- Use `PATCH /api/v1/workflows/{workflowId}/status` to move old versions to `ARCHIVED`.

## 4. Start Work

Admins and FPO_MANAGERs can assign an activity timeline to a FIELD_COORDINATOR from the
Admin dashboard `Workflows` tab. FIELD_COORDINATORs can also start their own activity
from an active workflow.

FIELD_COORDINATORs can start an activity from an active workflow:

```http
POST /api/v1/activities
```

Body:

```json
{
  "workflowDefinitionId": "<workflow-id>",
  "unitName": "<farm/plot/site name>",
  "locationName": "<region/village/site>",
  "startedOn": "2026-05-05"
}
```

The backend creates the task timeline from the workflow definition.

## 5. FIELD_COORDINATOR Uploads Proof

Use:

```http
POST /api/v1/evidence
```

Send multipart form fields:

- `activityId`
- `activityTaskId`
- `file`
- `note`

The backend stores the file through the storage interface and records metadata in `evidence`.

## 6. Admin Reviews Proof

Use:

```http
PATCH /api/v1/evidence/{evidenceId}/review
```

Body:

```json
{
  "status": "APPROVED"
}
```

Use `REJECTED` when proof is not acceptable.

## 7. Generate A Report

Admins and FPO_MANAGERs can view report-ready metrics:

```http
GET /api/v1/reports/summary
```

The summary includes FIELD_COORDINATOR coverage, activity progress, proof review
counts, and workflow/location breakdowns.

To create a PDF or Excel export:

```http
POST /api/v1/reports/export
```

Body:

```json
{
  "format": "PDF",
  "reportType": "GOVERNMENT_EVIDENCE"
}
```

Use `"format": "XLSX"` for the spreadsheet version. The backend creates a
`report_exports` record, stores the generated file through the storage
interface, and returns the report `storageKey`. Both formats follow the
workflow/activity/task sequence and list proof file references for each task.
The Excel workbook also separates summary, activity, task evidence, workflow,
and location reporting sheets for admin/government use.

## 8. Manage Staff Roles

Admins use the Admin dashboard `Roles` tab to assign `ADMIN`, `FPO_MANAGER`, and
`FIELD_COORDINATOR` permissions. A FPO_MANAGER is an operational manager: they can
create workflows, assign activities, review evidence, export reports, and manage
notification status, but they cannot change user roles. See
[Roles And FPO_MANAGERs](roles-and-FPO_MANAGERs.md) for the full explanation.

Admins can list roles and update a user's assigned role set:

```http
GET /api/v1/roles
PUT /api/v1/users/{userId}/roles
```

Body:

```json
{
  "roles": ["FPO_MANAGER"]
}
```

FPO_MANAGERs can view role assignments, but only admins can change them. A user
must sign in again or refresh tokens after their roles change.

## 9. Track Notifications

Admins and FPO_MANAGERs can queue notification status records:

```http
POST /api/v1/notifications
GET /api/v1/notifications?status=QUEUED
PATCH /api/v1/notifications/{notificationId}/status
```

This gives the admin dashboard and future delivery workers a shared place to
track pending, sent, failed, or skipped messages before real SMS, email, push,
or in-app delivery adapters are connected.
