# API Standards

## REST Shape

Use `/api/v1` as the backend API root.

Recommended resources:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/me`
- `GET /api/v1/users`
- `POST /api/v1/users`
- `GET /api/v1/users/me`
- `GET /api/v1/users/{userId}`
- `PUT /api/v1/users/{userId}`
- `PATCH /api/v1/users/{userId}/status`
- `GET /api/v1/roles`
- `GET /api/v1/users/{userId}/roles`
- `PUT /api/v1/users/{userId}/roles`
- `GET /api/v1/workflows`
- `POST /api/v1/workflows`
- `PUT /api/v1/workflows/{workflowId}`
- `PATCH /api/v1/workflows/{workflowId}/status`
- `GET /api/v1/activities`
- `POST /api/v1/activities`
- `GET /api/v1/activities/{activityId}`
- `PATCH /api/v1/activities/{activityId}/tasks/{activityTaskId}/status`
- `GET /api/v1/evidence`
- `POST /api/v1/evidence`
- `PATCH /api/v1/evidence/{evidenceId}/review`
- `GET /api/v1/notifications`
- `POST /api/v1/notifications`
- `PATCH /api/v1/notifications/{notificationId}/status`
- `GET /api/v1/reports/summary`
- `POST /api/v1/reports/export`

## Response Envelope

Success:

```json
{
  "success": true,
  "data": {},
  "meta": {
    "requestId": "..."
  }
}
```

Paged lists use a stable wrapper instead of exposing framework-specific page
serialization:

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": {
      "page": 0,
      "size": 20,
      "totalElements": 0,
      "totalPages": 0
    }
  }
}
```

Failure:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed.",
    "details": {
      "field": ["reason"]
    },
    "traceId": "..."
  }
}
```

## Standards

- Use UUIDs for public identifiers.
- Include `tenantCode` on login where the client can provide it. Default local tenant is `default`.
- Use ISO-8601 timestamps in API payloads.
- Never expose stack traces in API responses.
- Return validation errors in a stable `details` map.
- Use role checks at controller/service boundaries.
- Log with request id, user id, tenant id, and activity id where available.
- Keep upload APIs multipart, but persist metadata and audit events transactionally.

## User Profile Creation

Admins and supervisors create field/farmer profiles through the generic user API.
Created users are tenant-scoped and receive the `PARTICIPANT` role.

```http
POST /api/v1/users
Authorization: Bearer <admin-or-supervisor-token>
Content-Type: application/json
```

```json
{
  "username": "farmer001",
  "password": "change-me-securely",
  "displayName": "Farmer Name",
  "phone": "+91 99999 00000",
  "locationName": "Village / district",
  "siteName": "Farm / plot name"
}
```

The response never includes `password` or `passwordHash`.

Admins and supervisors can update participant profile basics and activate or
deactivate a participant without changing workflow/activity history:

```http
GET /api/v1/users/me
PUT /api/v1/users/{userId}
PATCH /api/v1/users/{userId}/status
```

Participants use `GET /api/v1/users/me` to load their own profile. Frontend
self-signup is disabled; participant accounts are created by admins or
supervisors.

## Role Management

Admins and supervisors can view the tenant role catalog:

```http
GET /api/v1/roles
Authorization: Bearer <admin-or-supervisor-token>
```

They can also inspect a user's assigned roles:

```http
GET /api/v1/users/{userId}/roles
Authorization: Bearer <admin-or-supervisor-token>
```

Only admins can update user role assignments:

```http
PUT /api/v1/users/{userId}/roles
Authorization: Bearer <admin-token>
Content-Type: application/json
```

```json
{
  "roles": ["SUPERVISOR"]
}
```

Role updates are tenant-scoped and record a `USER_ROLES_UPDATED` audit event.
The API blocks admins from changing their own roles through this endpoint to
avoid accidental lockout. `PARTICIPANT` cannot be combined with staff roles.
JWT role claims change only after the affected user receives new tokens.

## Notification Status Tracking

Admins and supervisors can queue notification records and track their delivery
state. This is a storage/status foundation; actual email, SMS, push, or in-app
delivery adapters can be added behind the same records later.

```http
POST /api/v1/notifications
Authorization: Bearer <admin-or-supervisor-token>
Content-Type: application/json
```

```json
{
  "recipientUserId": "<optional-user-id>",
  "channel": "IN_APP",
  "templateCode": "EVIDENCE_REVIEWED",
  "payload": {
    "evidenceId": "<evidence-id>",
    "status": "APPROVED"
  }
}
```

List notifications with optional status filtering:

```http
GET /api/v1/notifications?status=QUEUED
```

Update status after a delivery attempt:

```http
PATCH /api/v1/notifications/{notificationId}/status
Content-Type: application/json
```

```json
{
  "status": "SENT"
}
```

Supported channels are `EMAIL`, `IN_APP`, `PUSH`, and `SMS`. Supported statuses
are `QUEUED`, `SENT`, `FAILED`, and `SKIPPED`. Queueing and status changes are
tenant-scoped and emit audit events.

## Storage Validation

All file writes go through the storage interface. Local disk and MinIO use the
same validation and object-key planning rules:

- size is greater than zero and no larger than `app.storage.max-upload-bytes`
- owner metadata is present and owner type is path-safe
- original filename is present, bounded to 255 characters, and does not contain path traversal
- resolved storage path stays inside the configured storage root
- extension and content type match an allowed pair

Allowed file pairs:

- `.jpg` / `.jpeg`: `image/jpeg` or `image/jpg`
- `.png`: `image/png`
- `.webp`: `image/webp`
- `.heic` / `.heif`: `image/heic` or `image/heif`
- `.pdf`: `application/pdf`
- `.xlsx`: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

Storage provider selection is configuration-driven:

```yaml
app:
  storage:
    provider: local # local or minio
    max-upload-bytes: 10485760
    local:
      root-path: ./storage/local
    minio:
      endpoint: https://minio.example.com
      bucket: activity-platform
      access-key: <access-key>
      secret-key: <secret-key>
      region: us-east-1
      secure: true
      create-bucket-if-missing: false
```

Stored object keys use the same structure for every provider:

```text
{tenantId}/{ownerType}/{ownerId}/{generatedUuid}.{extension}
```

In the `prod` Spring profile, startup validation requires MinIO storage and
rejects unsafe defaults before the API starts serving requests.

## Report Summary And Export

Admins and supervisors can view reusable reporting metrics:

```http
GET /api/v1/reports/summary
Authorization: Bearer <admin-or-supervisor-token>
```

The summary includes tenant-scoped participant counts, activity status counts,
task completion, evidence review counts, and workflow/location breakdowns.

Admins and supervisors can request a report export:

```http
POST /api/v1/reports/export
Authorization: Bearer <admin-or-supervisor-token>
Content-Type: application/json
```

```json
{
  "format": "PDF",
  "reportType": "GOVERNMENT_EVIDENCE"
}
```

Use `format: "XLSX"` for the government/admin spreadsheet export. Export
generation currently completes synchronously and returns a
`ReportExportResponse` with `status`, `storageKey`, `requestedAt`, and
`completedAt`. The PDF is ordered by workflow, participant, activity, and task
sequence, and includes evidence status, submitted date, reviewer state, and
stored proof file references. The XLSX workbook uses the same proof sequence and
adds separate sheets for summary metrics, activities, task evidence, workflow
breakdown, and location breakdown.
