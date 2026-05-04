# Developer Guide

This project is a reusable activity-compliance platform. Agriculture is the first client domain, but the core should stay generic enough for inspections, field work, warehouse checks, NGO programs, dairy operations, factory audits, and construction progress tracking.

## Mental Model

Use generic platform language inside shared code:

- `Tenant`: one client or organization.
- `User`: admin, supervisor, farmer, field worker, inspector, or participant.
- `Workflow`: configurable process definition.
- `Task`: one ordered step in a workflow.
- `Activity`: one user executing one workflow.
- `Evidence`: proof photo/file/note submitted for a task.
- `AuditEvent`: append-only record of important actions.
- `Report`: generated PDF/XLSX or dashboard summary.

Domain words like farmer, crop, plot, harvest, village, and government report belong in configuration, seed data, UI copy, and report labels.

## Repository Layout

```text
.
├── App.tsx
├── src/
│   ├── auth/              # temporary Expo demo auth facade
│   ├── core/              # reusable frontend contracts/helpers
│   ├── data/              # local prototype stores and agriculture seed data
│   ├── navigation/
│   ├── screens/
│   └── ui/
├── backend/
│   ├── compose.yaml       # local PostgreSQL
│   ├── pom.xml
│   └── src/main/java/com/activityplatform/backend/
│       ├── auth/
│       ├── security/
│       ├── workflow/
│       ├── activity/
│       ├── evidence/
│       ├── storage/
│       ├── audit/
│       ├── reporting/
│       ├── notification/
│       ├── platform/
│       └── common/
└── docs/
```

## Prerequisites

- Node.js compatible with Expo SDK 54.
- Java 21.
- Maven wrapper from `backend/mvnw.cmd`.
- Docker Desktop for local PostgreSQL.

## Frontend Quick Start

```powershell
npm install
npm run typecheck
npm run web
```

Demo accounts while the frontend still uses local auth:

- Admin: `admin` / `admin123`
- Farmer/participant: `user` / `user123`

The frontend currently runs at:

```text
http://localhost:19006
```

## Backend Quick Start

```powershell
cd backend
docker compose up -d
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Default database:

```text
jdbc:postgresql://localhost:5432/activity_platform
username: activity_app
password: activity_app
```

Seeded backend accounts:

- Tenant: `default`
- Admin: `admin` / `admin123`
- Participant: `user` / `user123`

## Backend Auth Flow

Login:

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "tenantCode": "default",
  "username": "admin",
  "password": "admin123"
}
```

The response returns an access token, refresh token, expiry, user id, tenant id, and roles.

Authenticated calls use:

```http
Authorization: Bearer <accessToken>
```

Current user:

```http
GET /api/v1/auth/me
```

Refresh:

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "<refreshToken>"
}
```

## Backend Module Responsibilities

- `common`: API envelope, error handling, request tracing, shared web concerns.
- `security`: JWT resource-server config, role model, authority conversion.
- `auth`: login, refresh, seeded users, tenant/user/role persistence.
- `workflow`: configurable workflow/task rules and progress calculation.
- `activity`: workflow execution instances and task timeline state.
- `evidence`: proof status, review flow, uploaded file metadata.
- `storage`: local storage adapter now, MinIO adapter later.
- `audit`: append-only compliance trail.
- `reporting`: PDF/XLSX export and report history.
- `notification`: queued notification framework.

## Coding Standards

- Keep controllers thin: request mapping, validation, response only.
- Put business rules in services with explicit transaction boundaries.
- Keep persistence in repositories/entities.
- Use `ApiResponse<T>` for API output.
- Throw `ApplicationException` for expected business errors.
- Never expose stack traces through API responses.
- Add `requestId`, `tenantId`, `userId`, and relevant aggregate ids to logs where available.
- Use UUIDs for public identifiers.
- Use ISO-8601 timestamps in API payloads.
- Keep workflow stages data-driven, not hardcoded.

## Transaction Boundaries

Use `@Transactional` on service methods:

- `readOnly = true` for queries.
- Default transaction for state changes.
- Evidence upload should persist metadata and audit state consistently. File write and DB transaction cannot be perfectly atomic, so store metadata only after the file write succeeds and emit audit after state change.

## Security Rules

- Default API behavior is authenticated.
- Public endpoints must be explicitly listed in `SecurityConfig`.
- Map JWT `roles` claim to Spring authorities as `ROLE_ADMIN`, `ROLE_SUPERVISOR`, and `ROLE_PARTICIPANT`.
- Do not store raw passwords.
- Development JWT secret is only for local use. Production must set `APP_JWT_SECRET`.

## Database Rules

- Schema changes go through Flyway migrations in `backend/src/main/resources/db/migration`.
- Do not let Hibernate mutate schema in runtime environments.
- Keep tenant ids on reusable business tables from day one.
- Use lookup/config tables for workflow definitions and task order.

## Testing

Frontend:

```powershell
npm run typecheck
```

Backend:

```powershell
cd backend
.\mvnw.cmd test
```

Current backend tests cover:

- API response envelope.
- Workflow progression.
- JWT issuing/refresh claim behavior.
- JWT role-to-authority conversion.

## How To Add A New Client Domain

1. Add or seed a tenant.
2. Add workflow definitions and workflow tasks.
3. Keep new UI labels/configuration domain-specific.
4. Reuse `Activity`, `Task`, `Evidence`, `AuditEvent`, and `Report` backend concepts.
5. Add report filters/labels for that client without changing core workflow logic.

## Near-Term Roadmap

1. Connect Expo login to backend auth.
2. Add workflow definition APIs.
3. Add activity start/timeline APIs.
4. Add evidence upload API using local storage.
5. Add audit events for login, activity changes, and evidence submissions.
6. Add admin report summary and export endpoints.

