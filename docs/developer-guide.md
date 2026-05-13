# Developer Guide

## Project Summary

This repository contains a reusable activity-compliance platform:

- Frontend: Expo React Native with TypeScript.
- Backend: Java 21, Spring Boot 4, Spring Security, JWT.
- Database: PostgreSQL with Flyway migrations.
- File storage: shared validation with local development storage and MinIO for
  production-compatible object storage.
- Reports: summary APIs plus PDF/Excel export generation from workflow,
  activity, task, and evidence data.

Agriculture is the first domain. The reusable core should stay generic enough
for other client workflows such as warehouse inspections, construction progress,
NGO field activity tracking, dairy operations, factory audits, and field worker
tracking.

For the detailed FPO-specific roadmap, current coverage, and developer task
breakdown, see [FPO MVP Roadmap](fpo-mvp-roadmap.md). That document is internal
planning material and should not be shared with a client before commercial
agreement.

For execution-level tasks, use
[FPO Developer Task List](fpo-developer-task-list.md).

For clean reset commands, current completion percentages, and open go-live gaps,
use [Clean Start Runbook](clean-start-runbook.md) and
[Project Status And Gap Register](project-status-and-gap-register.md).

For module subscriptions, feature gating, source handover risk, and the
modular-monolith-first decision, see
[Modular Platform Strategy](modular-platform-strategy.md).

## Repository Layout

```text
.
|-- App.tsx
|-- package.json
|-- src/
|   |-- auth/
|   |-- core/
|   |   |-- api/
|   |   |-- config/
|   |   |-- errors/
|   |   |-- logging/
|   |   |-- model/
|   |   |-- storage/
|   |   `-- workflow/
|   |-- data/
|   |-- navigation/
|   |-- screens/
|   `-- ui/
|-- backend/
|   |-- compose.yaml
|   |-- pom.xml
|   |-- src/main/java/com/activityplatform/backend/
|   |   |-- activity/
|   |   |-- audit/
|   |   |-- auth/
|   |   |-- common/
|   |   |-- evidence/
|   |   |-- notification/
|   |   |-- fpo/
|   |   |-- platform/
|   |   |-- reporting/
|   |   |-- security/
|   |   |-- storage/
|   |   |-- user/
|   |   `-- workflow/
|   `-- src/main/resources/db/migration/
`-- docs/
```

## Required Tools

- Node.js and npm.
- Java 21.
- Docker Desktop or Docker Engine.
- Maven wrapper included in `backend`.

Optional:

- PostgreSQL client such as `psql`.
- Swagger UI through the running backend.

## Local Ports

Default local ports:

- Expo web: `19006`
- Spring Boot API: `8080`
- PostgreSQL host port: `5432`
- MinIO API: `9000`
- MinIO console: `9001`

Port changes are safe if config is kept consistent:

- Frontend API base URL is `EXPO_PUBLIC_API_BASE_URL` with a local fallback in
  `src/core/config/appConfig.ts`.
- Backend CORS origins are in `backend/src/main/resources/application-local.yml`
  or `APP_CORS_ALLOWED_ORIGIN_WEB`.

## Frontend Setup

From the repository root:

```powershell
npm install
npm run typecheck
npm run lint
npm run web
```

Open:

```text
http://localhost:19006
```

The app stores JWT/session data with AsyncStorage. Login tries the backend first.
Self-signup is disabled; admins and supervisors create participant accounts
through backend user management. Participant profile display is backend-first via
`GET /api/v1/users/me`, with local fallback only for development/offline
prototype use.

Production and Docker builds should set:

```powershell
$env:EXPO_PUBLIC_API_BASE_URL="https://api.example.com"
$env:EXPO_PUBLIC_API_VERSION="v1"
$env:EXPO_PUBLIC_DEFAULT_TENANT_CODE="default"
```

## Backend Setup

From the repository root:

```powershell
cd backend
docker compose up -d
$env:SPRING_PROFILES_ACTIVE="local"
.\mvnw.cmd spring-boot:run
```

Backend health:

```text
http://localhost:8080/actuator/health
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Default database from `backend/compose.yaml`:

```text
database: activity_platform
username: activity_app
password: activity_app
host port: 5432
container port: 5432
```

## Local Seed Users

The base `application.yml` keeps seed users disabled. The `local` profile enables
local seed users.

Default local seed values:

```text
tenantCode: default
admin username: admin
admin password: arun
participant username: participant
participant password: local-participant-password
```

You can override them before starting the backend:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
$env:APP_SEED_ADMIN_USERNAME="admin"
$env:APP_SEED_ADMIN_PASSWORD="your-password"
$env:APP_SEED_PARTICIPANT_USERNAME="participant"
$env:APP_SEED_PARTICIPANT_PASSWORD="your-password"
```

Important: if a user already exists in the database, seeding does not overwrite
that user's password. Reset the local DB volume or update the user password if
you need a different local password.

## Common Local Commands

Start PostgreSQL:

```powershell
cd backend
docker compose up -d
```

Stop PostgreSQL:

```powershell
cd backend
docker compose down
```

Reset local PostgreSQL data:

```powershell
cd backend
docker compose down -v
docker compose up -d
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

Run frontend checks:

```powershell
npm run typecheck
npm run lint
```

## Environment Variables

Backend:

| Variable                      | Purpose                                             |
| ----------------------------- | --------------------------------------------------- |
| `SPRING_PROFILES_ACTIVE`      | Use `local` for local seed/config.                  |
| `APP_PORT`                    | Spring Boot port, default `8080`.                   |
| `APP_POSTGRES_PORT`           | Local Docker PostgreSQL host port, default `5432`.  |
| `APP_DB_URL`                  | JDBC database URL.                                  |
| `APP_DB_USERNAME`             | Database username.                                  |
| `APP_DB_PASSWORD`             | Database password.                                  |
| `APP_JWT_SECRET`              | JWT signing secret. Required outside local/test.    |
| `APP_CORS_ALLOWED_ORIGINS`    | Comma/list style CORS origins in base config.       |
| `APP_CORS_ALLOWED_ORIGIN_WEB` | Local web origin, default `http://localhost:19006`. |
| `APP_SEED_ENABLED`            | Enable local seed data.                             |
| `APP_SEED_ADMIN_USERNAME`     | Local admin username.                               |
| `APP_SEED_ADMIN_PASSWORD`     | Local admin password.                               |
| `APP_LOCAL_STORAGE_PATH`      | Local file storage root path.                       |
| `APP_STORAGE_PROVIDER`        | `local` for dev/test, `minio` for production.       |
| `APP_MAX_UPLOAD_BYTES`        | Maximum proof upload size in bytes.                 |
| `APP_MINIO_ENDPOINT`          | MinIO/S3-compatible endpoint.                       |
| `APP_MINIO_BUCKET`            | Bucket for proof and export files.                  |
| `APP_MINIO_ACCESS_KEY`        | MinIO access key.                                   |
| `APP_MINIO_SECRET_KEY`        | MinIO secret key.                                   |
| `APP_MINIO_REGION`            | Optional MinIO/S3 region.                           |
| `APP_MINIO_SECURE`            | Whether generated MinIO endpoint uses HTTPS.        |
| `APP_LOG_LEVEL_ROOT`          | Production root log level.                          |
| `APP_LOG_LEVEL_APP`           | Application package log level.                      |
| `APP_OPENAPI_ENABLED`         | Enable OpenAPI JSON in production if needed.        |
| `APP_SWAGGER_UI_ENABLED`      | Enable Swagger UI in production if needed.          |

Frontend:

| Variable                            | Purpose                                      |
| ----------------------------------- | -------------------------------------------- |
| `EXPO_PUBLIC_APP_NAME`              | Display app name.                            |
| `EXPO_PUBLIC_API_BASE_URL`          | Backend API origin, default localhost.       |
| `EXPO_PUBLIC_API_VERSION`           | API version segment, default `v1`.           |
| `EXPO_PUBLIC_DEFAULT_TENANT_CODE`   | Default login tenant code for local usage.   |
| `EXPO_PUBLIC_DEFAULT_LOCALE`        | Locale default, currently `en-IN`.           |
| `EXPO_PUBLIC_STORAGE_NAMESPACE`     | Frontend storage namespace.                  |

## Authentication Flow

Login endpoint:

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "tenantCode": "default",
  "username": "admin",
  "password": "arun"
}
```

Authenticated requests:

```http
Authorization: Bearer <accessToken>
```

Current user:

```http
GET /api/v1/auth/me
```

Refresh token:

```http
POST /api/v1/auth/refresh
```

## Main API Areas

Auth:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/me`

User/admin participant management:

- `GET /api/v1/users`
- `POST /api/v1/users`
- `GET /api/v1/users/me`
- `GET /api/v1/users/{userId}`
- `PUT /api/v1/users/{userId}`
- `PATCH /api/v1/users/{userId}/status`

Workflow definitions:

- `GET /api/v1/workflows`
- `POST /api/v1/workflows`
- `PUT /api/v1/workflows/{workflowId}`
- `PATCH /api/v1/workflows/{workflowId}/status`

Activities:

- `GET /api/v1/activities`
- `POST /api/v1/activities`
- `GET /api/v1/activities/{activityId}`
- `PATCH /api/v1/activities/{activityId}/tasks/{activityTaskId}/status`

Evidence:

- `GET /api/v1/evidence`
- `POST /api/v1/evidence`
- `PATCH /api/v1/evidence/{evidenceId}/review`

Reporting:

- `GET /api/v1/reports/summary`
- `POST /api/v1/reports/export`

## Backend Coding Standards

Naming:

- Use generic names in core modules: `User`, `Workflow`, `Activity`, `Task`,
  `Evidence`, `Report`.
- Use domain names such as farmer/crop only in UI copy, seed data, report labels,
  and configuration.

Controllers:

- Keep controllers thin.
- Accept request DTO records.
- Validate input with Jakarta validation annotations.
- Return `ApiResponse<T>`.
- Use `PageResponse<T>` for paged lists.

Services:

- Own business rules.
- Own transaction boundaries.
- Use `@Transactional(readOnly = true)` for queries.
- Use default `@Transactional` for state changes.
- Emit audit events for state changes.

Repositories:

- Keep tenant-scoped query methods explicit.
- Avoid business rules in repositories.

Errors:

- Use `ApplicationException` for expected business errors.
- Use stable `ErrorCode` values.
- Never expose stack traces.

Logging:

- Include request id through `RequestTraceFilter`.
- Add tenant/user/aggregate ids where useful.
- Do not log passwords or tokens.

Security:

- Default API access is authenticated.
- Public endpoints must be explicitly allowed in `SecurityConfig`.
- Role checks should be visible at controller/service boundaries.
- Do not store raw passwords.
- Production must provide a strong JWT secret.

Database:

- Schema changes go through Flyway.
- Keep `ddl-auto=validate`.
- Include tenant ids on durable business tables.
- Keep workflows configurable through tables.

## Frontend Coding Standards

- Keep API endpoints centralized in `src/core/api/endpoints.ts`.
- Keep response contracts in `src/core/api/contracts.ts`.
- Use `AppError` for user-facing failures.
- Keep local prototype storage isolated under `src/data`.
- Do not add public/self-service signup unless the backend product decision
  explicitly allows it.
- Keep reusable app types in `src/core/model/types.ts`.
- Do not expose local/default credentials in screens.
- Avoid adding domain-specific assumptions to core folders.

## Adding A New Backend Feature

1. Add/update Flyway migration if schema is needed.
2. Add entity/repository methods.
3. Add request/response DTOs under the module `api` package.
4. Add service method with transaction boundary.
5. Add audit event for state-changing behavior.
6. Add controller endpoint.
7. Add unit tests for pure service logic if applicable.
8. Add integration tests for HTTP/security/database behavior.
9. Update API docs and developer guide.

## Adding A New Client Domain

1. Create or seed a tenant.
2. Add workflow definitions and task templates.
3. Add UI labels/configuration for the client domain.
4. Reuse user/activity/evidence/audit/report concepts.
5. Add report filters and export labels without changing core logic.

## Known Current Development State

For the live completion percentages and gap list, use
[Project Status And Gap Register](project-status-and-gap-register.md). Keep this
section as a short implementation summary only.

Implemented foundation:

- JWT login/refresh/current user.
- Role-based security.
- Tenant-aware users and roles.
- Admin/supervisor participant profile management.
- Backend-owned participant profile display through `GET /api/v1/users/me`.
- Platform module subscriptions and module guards.
- Configurable workflow definitions.
- Activity timeline tracking.
- Evidence metadata/upload foundation.
- Audit events for important state changes.
- Local PostgreSQL compose setup.
- Frontend admin participant creation connected to backend.
- FPO member management backend and admin UI.
- FPO landholding and farm plot backend and admin UI.
- FPO crop catalog, season, crop history, and seasonal crop plan backend and UI.
- FPO input catalog, input rule, demand calculation, and demand summary backend
  and UI.
- FPO dashboard summary and Excel export foundation.
- FPO advisory backend and admin UI.
- Carbon app-flow prototype screens backed by dummy frontend data.

Current production-readiness coverage:

- Frontend workflow, activity, proof upload, evidence review, reports, roles,
  and notifications are backend-first.
- Backend summary, PDF export, and Excel export APIs are implemented.
- Local and MinIO storage providers share the same validation/key planning.
- Production profile validates DB, JWT, CORS, and MinIO safety at startup.
- Docker Compose starts PostgreSQL, MinIO, backend, and Expo web.
- CI covers frontend typecheck/lint, backend unit tests, backend integration
  tests, Docker Compose config validation, npm audit, and OWASP Dependency
  Check.

Remaining before a client production handoff:

- Run client UAT with real workflow definitions and real export templates.
- Freeze FPO data dictionary, input formulas, and report layouts.
- Add frontend automated tests and browser/E2E smoke coverage.
- Add MinIO/S3-compatible storage integration tests.
- Configure managed secrets, backups, monitoring, alerting, and log retention.
- Decide whether production OpenAPI/Swagger should remain disabled or be exposed
  only through protected internal access.
- Pin infrastructure image versions and retention policies for the target
  hosting environment.
