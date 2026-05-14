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
- `GET /api/v1/platform/modules`
- `GET /api/v1/platform/modules/enabled`
- `GET /api/v1/platform/module-subscriptions`
- `PUT /api/v1/platform/module-subscriptions/{moduleCode}`
- `GET /api/v1/fpo/members`
- `POST /api/v1/fpo/members`
- `GET /api/v1/fpo/members/me`
- `GET /api/v1/fpo/members/{memberId}`
- `PUT /api/v1/fpo/members/{memberId}`
- `PATCH /api/v1/fpo/members/{memberId}/status`
- `GET /api/v1/fpo/members/{memberId}/landholdings`
- `POST /api/v1/fpo/members/{memberId}/landholdings`
- `PUT /api/v1/fpo/landholdings/{landholdingId}`
- `PATCH /api/v1/fpo/landholdings/{landholdingId}/status`
- `GET /api/v1/fpo/members/{memberId}/plots`
- `POST /api/v1/fpo/members/{memberId}/plots`
- `PUT /api/v1/fpo/plots/{plotId}`
- `PATCH /api/v1/fpo/plots/{plotId}/status`
- `GET /api/v1/fpo/crops`
- `POST /api/v1/fpo/crops`
- `PUT /api/v1/fpo/crops/{cropId}`
- `PATCH /api/v1/fpo/crops/{cropId}/status`
- `GET /api/v1/fpo/seasons`
- `POST /api/v1/fpo/seasons`
- `PUT /api/v1/fpo/seasons/{seasonId}`
- `PATCH /api/v1/fpo/seasons/{seasonId}/status`
- `GET /api/v1/fpo/members/{memberId}/crop-history`
- `POST /api/v1/fpo/members/{memberId}/crop-history`
- `PUT /api/v1/fpo/crop-history/{historyId}`
- `GET /api/v1/fpo/crop-plans`
- `POST /api/v1/fpo/crop-plans`
- `GET /api/v1/fpo/crop-plans/{planId}`
- `PUT /api/v1/fpo/crop-plans/{planId}`
- `PATCH /api/v1/fpo/crop-plans/{planId}/status`
- `GET /api/v1/fpo/inputs`
- `POST /api/v1/fpo/inputs`
- `PUT /api/v1/fpo/inputs/{inputId}`
- `PATCH /api/v1/fpo/inputs/{inputId}/status`
- `GET /api/v1/fpo/input-rules`
- `POST /api/v1/fpo/input-rules`
- `PUT /api/v1/fpo/input-rules/{ruleId}`
- `PATCH /api/v1/fpo/input-rules/{ruleId}/status`
- `POST /api/v1/fpo/demand-estimates/run`
- `GET /api/v1/fpo/demand-estimates`
- `GET /api/v1/fpo/demand-estimates/summary`
- `GET /api/v1/fpo/advisories`
- `POST /api/v1/fpo/advisories`
- `GET /api/v1/fpo/advisories/{advisoryId}`
- `PATCH /api/v1/fpo/advisories/{advisoryId}/status`
- `GET /api/v1/fpo/reports/summary`
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

Admins and FPO_MANAGERs create field/farmer profiles through the generic user API.
Created users are tenant-scoped and receive the `FIELD_COORDINATOR` role.

```http
POST /api/v1/users
Authorization: Bearer <admin-FPO_MANAGER-or-FIELD_COORDINATOR-token>
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

Admins and FPO_MANAGERs can update FIELD_COORDINATOR profile basics and activate or
deactivate a FIELD_COORDINATOR without changing workflow/activity history:

```http
GET /api/v1/users/me
PUT /api/v1/users/{userId}
PATCH /api/v1/users/{userId}/status
```

FIELD_COORDINATORs use `GET /api/v1/users/me` to load their own profile. Frontend
self-signup is disabled. For FPO Phase 1, farmer profiles are created as FPO
member records and do not require login users.

## Role Management

Admins can view and manage the role catalog. FPO managers may receive a scoped
view for their own FPO during Phase 1 role-alignment work:

```http
GET /api/v1/roles
Authorization: Bearer <admin-token>
```

They can also inspect a user's assigned roles:

```http
GET /api/v1/users/{userId}/roles
Authorization: Bearer <admin-token>
```

Only admins can update user role assignments:

```http
PUT /api/v1/users/{userId}/roles
Authorization: Bearer <admin-token>
Content-Type: application/json
```

```json
{
  "roles": ["FPO_MANAGER"]
}
```

Role updates are tenant-scoped and record a `USER_ROLES_UPDATED` audit event.
The API blocks admins from changing their own roles through this endpoint to
avoid accidental lockout. For FPO Phase 1, use `ADMIN`, `FPO_MANAGER`, and
`FIELD_COORDINATOR`; do not use the legacy `FPO_MANAGER` label for new FPO work.
JWT role claims change only after the affected user receives new tokens.

## Module Subscriptions

The platform supports tenant-level module subscriptions so a client can buy one
service without receiving every platform capability.

Public catalog:

```http
GET /api/v1/platform/modules
```

Authenticated current-tenant module list:

```http
GET /api/v1/platform/modules/enabled
Authorization: Bearer <access-token>
```

```json
{
  "modules": ["MEMBER_DATA", "LAND_RECORDS", "REPORT_EXPORT"]
}
```

Admin-only subscription management:

```http
GET /api/v1/platform/module-subscriptions
PUT /api/v1/platform/module-subscriptions/{moduleCode}
Authorization: Bearer <admin-token>
Content-Type: application/json
```

```json
{
  "status": "ENABLED",
  "expiresAt": null
}
```

Backend feature APIs must enforce module access server-side. Disabled modules
return:

```json
{
  "success": false,
  "error": {
    "code": "MODULE_NOT_ENABLED",
    "message": "Module MEMBER_DATA is not enabled for this tenant."
  }
}
```

## FPO Member APIs

FPO member profiles extend platform users with farmer/member-specific fields.
They are guarded by the `MEMBER_DATA` module.

Admins and FPO_MANAGERs can list and create members:

```http
GET /api/v1/fpo/members
POST /api/v1/fpo/members
Authorization: Bearer <admin-FPO_MANAGER-or-FIELD_COORDINATOR-token>
Content-Type: application/json
```

Create with an existing FIELD_COORDINATOR user:

```json
{
  "userId": "<FIELD_COORDINATOR-user-id>",
  "memberNumber": "MEM-001",
  "displayName": "Farmer Name",
  "mobileNumber": "+91 99999 00000",
  "alternateMobileNumber": "+91 88888 00000",
  "village": "Village name",
  "blockName": "Block name",
  "districtName": "District name",
  "gender": "FEMALE",
  "age": 34,
  "farmerCategory": "SMALL",
  "coordinatorUserId": "<admin-or-FPO_MANAGER-user-id>",
  "status": "ACTIVE"
}
```

Create and link a new FIELD_COORDINATOR login in one request by sending `username` and
`password` instead of `userId`.

```json
{
  "username": "farmer001",
  "password": "change-me-securely",
  "memberNumber": "MEM-001",
  "displayName": "Farmer Name",
  "mobileNumber": "+91 99999 00000",
  "village": "Village name"
}
```

Member read/update/status endpoints:

```http
GET /api/v1/fpo/members/me
GET /api/v1/fpo/members/{memberId}
PUT /api/v1/fpo/members/{memberId}
PATCH /api/v1/fpo/members/{memberId}/status
```

Rules:

- Member numbers are unique per tenant.
- Mobile numbers are unique per tenant.
- A member links to one `FARMER` login user and may be assigned to one
  `FIELD_COORDINATOR`.
- Admins and FPO_MANAGERs can manage members.
- FIELD_COORDINATORs can manage assigned farmer profiles.
- FARMERs can read only their own member profile.
- Changes emit `FPO_MEMBER_CREATED`, `FPO_MEMBER_UPDATED`, or
  `FPO_MEMBER_STATUS_CHANGED` audit events.

## FPO Landholding And Plot APIs

FPO farm asset APIs are guarded by the `LAND_RECORDS` module. Admins and
FPO_MANAGERs can manage records. FIELD_COORDINATORs can manage assigned farmer
records. FARMERs can read their own records.

Landholdings:

```http
GET /api/v1/fpo/members/{memberId}/landholdings
POST /api/v1/fpo/members/{memberId}/landholdings
PUT /api/v1/fpo/landholdings/{landholdingId}
PATCH /api/v1/fpo/landholdings/{landholdingId}/status
Authorization: Bearer <admin-or-FPO_MANAGER-token>
Content-Type: application/json
```

```json
{
  "surveyNumber": "SUR-101",
  "totalAreaAcres": 3.5,
  "cultivableAreaAcres": 2.75,
  "ownershipType": "Self-owned",
  "irrigationSource": "Canal",
  "status": "ACTIVE"
}
```

Plots:

```http
GET /api/v1/fpo/members/{memberId}/plots
POST /api/v1/fpo/members/{memberId}/plots
PUT /api/v1/fpo/plots/{plotId}
PATCH /api/v1/fpo/plots/{plotId}/status
Authorization: Bearer <admin-or-FPO_MANAGER-token>
Content-Type: application/json
```

```json
{
  "landholdingId": "<optional-landholding-id>",
  "plotName": "North plot",
  "areaAcres": 0.75,
  "latitude": 26.8501234,
  "longitude": 80.9491234,
  "soilType": "LOAM",
  "status": "ACTIVE"
}
```

Rules:

- `totalAreaAcres` and `areaAcres` must be greater than zero.
- `cultivableAreaAcres` cannot exceed `totalAreaAcres`.
- `surveyNumber`, `ownershipType`, and `irrigationSource` are required.
- Approved ownership values are `Self-owned`, `Leased-in`, and `Sharecropper`.
- Approved irrigation values are `Canal`, `Borewell`, `Open well`, `Pond`, `Rainfed`, and `Drip`.
- Latitude is required and must be between `-90` and `90`.
- Longitude is required and must be between `-180` and `180`.
- A plot can reference only a landholding owned by the same FPO member.
- Lifecycle status values are `ACTIVE`, `INACTIVE`, and `ARCHIVED`.
- Changes emit `FPO_LANDHOLDING_*` and `FPO_PLOT_*` audit events.

## FPO Crop Planning APIs

Crop planning APIs are guarded by the `CROP_PLANNING` module. Admins and
FPO_MANAGERs can manage records. FIELD_COORDINATORs can read their own crop history
and crop plans.

Crop catalog:

```http
GET /api/v1/fpo/crops
POST /api/v1/fpo/crops
PUT /api/v1/fpo/crops/{cropId}
PATCH /api/v1/fpo/crops/{cropId}/status
Authorization: Bearer <admin-or-FPO_MANAGER-token>
Content-Type: application/json
```

```json
{
  "code": "TOM",
  "name": "Tomato",
  "category": "Vegetable",
  "status": "ACTIVE"
}
```

Seasons:

```http
GET /api/v1/fpo/seasons
POST /api/v1/fpo/seasons
PUT /api/v1/fpo/seasons/{seasonId}
PATCH /api/v1/fpo/seasons/{seasonId}/status
Authorization: Bearer <admin-or-FPO_MANAGER-token>
Content-Type: application/json
```

```json
{
  "code": "KHA",
  "name": "Kharif",
  "startMonth": 6,
  "endMonth": 9,
  "seasonYear": 2026,
  "status": "ACTIVE"
}
```

Crop history:

```http
GET /api/v1/fpo/members/{memberId}/crop-history
POST /api/v1/fpo/members/{memberId}/crop-history
PUT /api/v1/fpo/crop-history/{historyId}
Authorization: Bearer <admin-or-FPO_MANAGER-token>
Content-Type: application/json
```

```json
{
  "cropId": "<crop-id>",
  "seasonId": "<optional-season-id>",
  "cropYear": 2025,
  "areaAcres": 1.25,
  "yieldQuantity": 18.5,
  "yieldUnit": "QUINTAL",
  "notes": "Good yield"
}
```

Seasonal crop plans:

```http
GET /api/v1/fpo/crop-plans?seasonId=<season-id>&cropId=<crop-id>&status=CONFIRMED
POST /api/v1/fpo/crop-plans
GET /api/v1/fpo/crop-plans/{planId}
PUT /api/v1/fpo/crop-plans/{planId}
PATCH /api/v1/fpo/crop-plans/{planId}/status
Authorization: Bearer <admin-or-FPO_MANAGER-token>
Content-Type: application/json
```

```json
{
  "memberId": "<member-id>",
  "plotId": "<optional-plot-id>",
  "cropId": "<crop-id>",
  "seasonId": "<season-id>",
  "cropYear": "2026-27",
  "plannedAreaAcres": 1.5,
  "plannedSowingDate": "2026-06-01",
  "expectedHarvestDate": "2026-09-30",
  "expectedYieldQuintals": 24.5,
  "status": "DRAFT"
}
```

Rules:

- Crop and season codes are unique per tenant.
- Crop and season status values are `ACTIVE`, `INACTIVE`, and `ARCHIVED`.
- Crop plan status values are `DRAFT`, `CONFIRMED`, `CANCELLED`, and
  `COMPLETED`.
- Crop plans must carry a crop year label such as `2026-27`, not only the
  season calendar year.
- `expectedYieldQuintals` is optional and manually entered when available.
- `confirmedAt` is set when a plan transitions to `CONFIRMED`; it drives the
  Phase 1 input demand report date filter.
- Crop history and crop plans must reference active crop and season records.
- If a crop plan references a plot, the plot must belong to the selected member
  and be active.
- Planned acreage cannot exceed selected active plot area.
- Expected harvest date cannot be before planned sowing date.
- Changes emit `FPO_CROP_*`, `FPO_SEASON_*`, `FPO_CROP_HISTORY_*`, and
  `FPO_CROP_PLAN_*` audit events.

## FPO Input Demand APIs

Input demand APIs are guarded by the `INPUT_DEMAND` module. Admins and
FPO_MANAGERs can manage input records, crop input rules, and demand calculations.

Input catalog:

```http
GET /api/v1/fpo/inputs
POST /api/v1/fpo/inputs
PUT /api/v1/fpo/inputs/{inputId}
PATCH /api/v1/fpo/inputs/{inputId}/status
Authorization: Bearer <admin-or-FPO_MANAGER-token>
Content-Type: application/json
```

```json
{
  "code": "NPK",
  "name": "NPK 19",
  "category": "Fertilizer",
  "unit": "KG",
  "status": "ACTIVE"
}
```

Crop input rules:

```http
GET /api/v1/fpo/input-rules?cropId=<crop-id>&inputId=<input-id>&status=ACTIVE
POST /api/v1/fpo/input-rules
PUT /api/v1/fpo/input-rules/{ruleId}
PATCH /api/v1/fpo/input-rules/{ruleId}/status
Authorization: Bearer <admin-or-FPO_MANAGER-token>
Content-Type: application/json
```

```json
{
  "cropId": "<crop-id>",
  "inputId": "<input-id>",
  "quantityPerAcre": 2.5,
  "applicationStage": "Basal",
  "notes": "First application",
  "status": "ACTIVE"
}
```

Demand calculation and reads:

```http
POST /api/v1/fpo/demand-estimates/run
GET /api/v1/fpo/demand-estimates?seasonId=<season-id>&cropId=<crop-id>&village=Village
GET /api/v1/fpo/demand-estimates/summary?seasonId=<season-id>&cropId=<crop-id>&village=Village
Authorization: Bearer <admin-or-FPO_MANAGER-token>
Content-Type: application/json
```

```json
{
  "seasonId": "<season-id>",
  "cropId": "<optional-crop-id>",
  "village": "Village",
  "planStatus": "CONFIRMED"
}
```

Rules:

- Input codes and units are normalized to uppercase and unique per tenant.
- Rules require active crop and input records.
- Duplicate rules are blocked per tenant, crop, input, and application stage.
- Calculation defaults to confirmed crop plans.
- Non-confirmed `planStatus` values are rejected in Phase 1.
- Multiple active stages for the same crop/input are summed into one estimate
  row per crop plan and input.
- Missing rules are counted in the run response and skipped.
- Demand estimates include member, village, crop, season, input, unit, status,
  submitted calculation timestamps, recommended quantity per acre, total demand,
  fixed 5% buffer, and rounded final demand.
- `estimatedQuantity` is retained as the displayed final demand quantity.
- Changes emit `FPO_INPUT_*`, `FPO_INPUT_RULE_*`, and
  `FPO_INPUT_DEMAND_CALCULATED` audit events.

## FPO Advisory APIs

Advisory APIs are guarded by the `ADVISORY` module. Admins and FPO_MANAGERs can
create and publish advisory records. FIELD_COORDINATORs can read only published
advisories targeted to all members, their village, or their own member profile.

```http
GET /api/v1/fpo/advisories?status=PUBLISHED&cropId=<crop-id>&seasonId=<season-id>
POST /api/v1/fpo/advisories
GET /api/v1/fpo/advisories/{advisoryId}
PATCH /api/v1/fpo/advisories/{advisoryId}/status
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "cropId": "<optional-crop-id>",
  "seasonId": "<optional-season-id>",
  "targetType": "VILLAGE",
  "targetVillage": "Village",
  "title": "Irrigation advisory",
  "message": "Irrigate onion plots in the evening this week.",
  "channel": "IN_APP",
  "status": "PUBLISHED"
}
```

Rules:

- `targetType` values are `ALL_MEMBERS`, `VILLAGE`, and `MEMBER`.
- `VILLAGE` advisories require `targetVillage`.
- `MEMBER` advisories require `targetMemberId`.
- Optional crop and season context must reference active records.
- Advisory status values are `DRAFT`, `PUBLISHED`, and `ARCHIVED`.
- Phase 1 stores the channel/status record only; it does not promise SMS,
  WhatsApp, email, or push delivery.
- Changes emit `FPO_ADVISORY_CREATED` and `FPO_ADVISORY_STATUS_CHANGED` audit
  events.

## FPO Report Summary API

The FPO dashboard summary API is guarded by the `REPORT_EXPORT` module. Admins
and FPO_MANAGERs can read consolidated FPO planning metrics:

```http
GET /api/v1/fpo/reports/summary
Authorization: Bearer <admin-or-FPO_MANAGER-token>
```

The response includes:

- total and active members
- total and active landholding counts
- total, active, and cultivable land acreage
- total and active plot counts
- geo-tagged active plot count
- total and active plot acreage
- total and confirmed crop plan counts
- confirmed planned acreage
- demand estimate count
- crop plan area by crop, season, and village
- input demand totals by input and unit

Rules:

- The API reads existing FPO records and does not create report files.
- Crop plan area breakdowns use confirmed crop plans.
- Input demand totals use stored demand estimate snapshots.
- Reads emit `FPO_REPORT_SUMMARY_VIEWED`.

FPO Excel export:

```http
POST /api/v1/fpo/reports/export
Authorization: Bearer <admin-or-FPO_MANAGER-token>
Content-Type: application/json
```

```json
{
  "filters": {
    "village": "Wagholi",
    "crop": "Paddy",
    "season": "Kharif",
    "coordinator": "Coordinator Name"
  }
}
```

The Phase 1 workbook emits exactly three sheets:

- `Farmer Register`
- `Crop Plan Summary`
- `Input Demand`

Current implementation note: the approved sheet structure is implemented;
filter application and branding/footer presentation remain tracked as go-live
cleanup.

## Notification Status Tracking

Admins and FPO_MANAGERs can queue notification records and track their delivery
state. This is a storage/status foundation; actual email, SMS, push, or in-app
delivery adapters can be added behind the same records later.

```http
POST /api/v1/notifications
Authorization: Bearer <admin-or-FPO_MANAGER-token>
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

Admins and FPO_MANAGERs can view reusable reporting metrics:

```http
GET /api/v1/reports/summary
Authorization: Bearer <admin-or-FPO_MANAGER-token>
```

The summary includes tenant-scoped FIELD_COORDINATOR counts, activity status counts,
task completion, evidence review counts, and workflow/location breakdowns.

Admins and FPO_MANAGERs can request a report export:

```http
POST /api/v1/reports/export
Authorization: Bearer <admin-or-FPO_MANAGER-token>
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
`completedAt`. The PDF is ordered by workflow, FIELD_COORDINATOR, activity, and task
sequence, and includes evidence status, submitted date, reviewer state, and
stored proof file references. The XLSX workbook uses the same proof sequence and
adds separate sheets for summary metrics, activities, task evidence, workflow
breakdown, and location breakdown.
