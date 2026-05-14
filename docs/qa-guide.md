# QA Guide

## Purpose

This guide defines the current quality checks for the reusable
activity-compliance platform. It is not a final client acceptance test document.
It is a working QA guide for development, smoke testing, and basic integration
testing.

## Current QA Scope

Covered now:

- Frontend type checking.
- Frontend linting.
- Backend unit tests.
- Backend integration tests with PostgreSQL/Testcontainers.
- Local smoke testing for login and admin FIELD_COORDINATOR creation.
- Basic API smoke testing.

Not final yet:

- Full manual test case catalog.
- Browser/device matrix.
- Performance testing.
- Security penetration testing.
- Production disaster recovery testing.
- Final client sign-off scenarios.

For live test gaps and completion percentages, use
[Project Status And Gap Register](project-status-and-gap-register.md).

## Test Environment

Local services:

| Service         | Default URL / Port                      |
| --------------- | --------------------------------------- |
| Expo web        | `http://localhost:19006`                |
| Spring Boot API | `http://localhost:8080`                 |
| PostgreSQL      | `localhost:5432`                        |
| Swagger UI      | `http://localhost:8080/swagger-ui.html` |

Start database:

```powershell
cd backend
docker compose up -d
```

Start backend:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="local"
.\mvnw.cmd spring-boot:run
```

Start frontend:

```powershell
npm run web
```

## Automated Checks

Run from repository root:

```powershell
npm run typecheck
npm run lint
```

Run backend unit tests:

```powershell
cd backend
.\mvnw.cmd test
```

Run backend integration tests:

```powershell
cd backend
.\mvnw.cmd -Pintegration-test verify
```

Windows Docker Desktop note:

If Testcontainers cannot find Docker even though `docker info` works, point the
test process at the active Docker Desktop Linux engine for that PowerShell
session:

```powershell
$env:DOCKER_HOST="npipe:////./pipe/dockerDesktopLinuxEngine"
$env:TESTCONTAINERS_DOCKER_CLIENT_STRATEGY="org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy"
.\mvnw.cmd -Pintegration-test verify
```

Expected result:

- All commands exit successfully.
- No TypeScript errors.
- No ESLint errors.
- Backend test summary reports zero failures and zero errors.

## Backend Integration Coverage

Current integration tests cover:

- Activity list/start/not found/unauthorized.
- Evidence list/not found/upload validation/unauthorized.
- Platform module subscription and enabled-module flows.
- FPO member profile CRUD/status/security.
- FPO landholding and farm plot CRUD/status/security.
- FPO crop catalog, season, history, and seasonal plan flows.
- FPO input catalog, input rules, calculation, and summary flows.
- FPO report summary and export flows.
- User create/list/get/update/status/security.
- Workflow list/create/security/not found/unauthorized.

Current unit tests cover:

- API response envelope.
- JWT issue/refresh claims.
- JWT role conversion.
- FPO crop planning validation.
- FPO farm asset validation.
- FPO input demand calculation.
- FPO dashboard summary aggregation.
- FPO workbook generation.
- Workflow definition logic.
- Workflow progression logic.

## Local Smoke Test Checklist

### Smoke-01 Backend Health

Steps:

1. Start PostgreSQL.
2. Start backend with local profile.
3. Open `http://localhost:8080/actuator/health`.

Expected:

```json
{
  "status": "UP"
}
```

The response can include liveness/readiness groups depending on actuator output.

### Smoke-02 Admin Login

Steps:

1. Start frontend and backend.
2. Open `http://localhost:19006`.
3. Login with a seeded admin.

Expected:

- Admin dashboard opens.
- User is not sent to FIELD_COORDINATOR screen.
- Browser console has no API login failure.

### Smoke-03 Create FIELD_COORDINATOR

Steps:

1. Login as admin.
2. Open `FIELD_COORDINATORs` tab.
3. Enter FIELD_COORDINATOR details.
4. Click `Create profile`.

Expected:

- Form clears after successful creation.
- New FIELD_COORDINATOR appears in the list.
- Backend returns a user with `FIELD_COORDINATOR` role.
- Response does not include `password` or `passwordHash`.

### Smoke-04 Duplicate FIELD_COORDINATOR Username

Steps:

1. Create FIELD_COORDINATOR with username `qa-user`.
2. Try to create another FIELD_COORDINATOR with username `QA-USER`.

Expected:

- Backend returns `409`.
- UI shows a user-friendly duplicate username message.
- Only one FIELD_COORDINATOR exists for that username.

### Smoke-05 Activate / Deactivate FIELD_COORDINATOR

Steps:

1. Login as admin.
2. Open FIELD_COORDINATOR list.
3. Click `Deactivate` on an active FIELD_COORDINATOR.
4. Click `Activate`.

Expected:

- Status changes to `Inactive`, then back to `Active`.
- Backend writes audit events.
- FIELD_COORDINATOR record remains visible.

### Smoke-06 FIELD_COORDINATOR Login

Steps:

1. Create a FIELD_COORDINATOR with known password.
2. Logout admin.
3. Login as the FIELD_COORDINATOR.

Expected:

- FIELD_COORDINATOR dashboard opens.
- Admin-only FIELD_COORDINATOR management is not available.

### Smoke-07 FIELD_COORDINATOR Profile Loads From Backend

Steps:

1. Create or use a backend FIELD_COORDINATOR.
2. Login as that FIELD_COORDINATOR.
3. Open the `Profile` tab.

Expected:

- Profile shows backend fields from `GET /api/v1/users/me`.
- Name, phone, region, village/site, and status match the backend user record.
- The login screen does not offer self-registration.

### Smoke-08 Unauthorized Access

Steps:

1. Call an authenticated API without token.
2. Call admin user creation as FIELD_COORDINATOR.

Expected:

- Missing token returns `401`.
- FIELD_COORDINATOR role returns `403`.

## API Smoke Testing With PowerShell

Login:

```powershell
$loginBody = @{
  tenantCode = "default"
  username = "admin"
  password = "arun"
} | ConvertTo-Json

$login = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body $loginBody

$token = $login.data.accessToken
```

List users:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/v1/users" `
  -Headers @{ Authorization = "Bearer $token" }
```

Current profile:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/v1/users/me" `
  -Headers @{ Authorization = "Bearer $token" }
```

Create FIELD_COORDINATOR:

```powershell
$FIELD_COORDINATORBody = @{
  username = "qa-FIELD_COORDINATOR"
  password = "password123"
  displayName = "QA FIELD_COORDINATOR"
  phone = "+91 90000 00000"
  locationName = "QA Region"
  siteName = "QA Village"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/users" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body $FIELD_COORDINATORBody
```

Deactivate FIELD_COORDINATOR:

```powershell
$statusBody = @{ status = "INACTIVE" } | ConvertTo-Json

Invoke-RestMethod `
  -Method Patch `
  -Uri "http://localhost:8080/api/v1/users/<userId>/status" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body $statusBody
```

## Regression Checklist Before Handoff

Run:

```powershell
npm run typecheck
npm run lint
cd backend
.\mvnw.cmd test
.\mvnw.cmd -Pintegration-test verify
```

Manual smoke:

- Admin login.
- FIELD_COORDINATOR creation.
- Duplicate username handling.
- FIELD_COORDINATOR activation/deactivation.
- FIELD_COORDINATOR login.
- FIELD_COORDINATOR profile tab reads backend profile.
- Backend health.

## Defect Reporting Format

Use this structure when logging bugs:

```text
Title:
Environment:
Build/branch:
User/role:
Steps to reproduce:
Expected result:
Actual result:
Screenshots/logs:
Severity:
Notes:
```

Severity guidance:

- Critical: data loss, security bypass, system unavailable.
- High: core workflow blocked.
- Medium: workaround exists but feature is impaired.
- Low: polish, copy, layout, minor inconsistency.

## Test Data Guidance

Use predictable prefixes:

- `qa-admin-*`
- `qa-FIELD_COORDINATOR-*`
- `qa-workflow-*`
- `qa-activity-*`

Do not use real farmer/client personal data in development or screenshots.

## Production QA Items To Add Later

- Full manual test cases by module.
- Browser and device matrix.
- API contract tests.
- Frontend component/screen tests.
- Browser/E2E smoke tests.
- Load testing for evidence uploads and reports.
- Backup/restore test.
- MinIO storage integration test.
- Object storage Testcontainers coverage if MinIO remains production-compatible
  storage.
- Jacoco coverage thresholds if CI should enforce a numeric quality floor.
- Security test checklist.
- Production deployment verification checklist.
- Client UAT sign-off scenarios.
