# Activity Compliance Backend

Spring Boot backend for the reusable activity-compliance platform.

## Stack

- Java 21
- Spring Boot 4
- Maven
- PostgreSQL
- Flyway
- Spring Security
- Testcontainers-ready test setup

## Package Structure

- `auth`: login/JWT contracts and authentication flow.
- `security`: role model and stateless API security.
- `platform`: public platform/module metadata.
- `workflow`: reusable workflow and task definition logic.
- `activity`: workflow execution and timeline tracking.
- `evidence`: proof/photo metadata and review status.
- `storage`: local storage adapter now, MinIO adapter later.
- `audit`: append-only compliance events.
- `reporting`: PDF/Excel export foundation.
- `notification`: notification delivery foundation.
- `common`: API envelope, errors, logging, request tracing.

## Local Database

Default connection:

```text
jdbc:postgresql://localhost:5432/activity_platform
username: activity_app
password: activity_app
```

Override with:

```powershell
$env:APP_DB_URL="jdbc:postgresql://localhost:5432/activity_platform"
$env:APP_DB_USERNAME="activity_app"
$env:APP_DB_PASSWORD="activity_app"
```

## Commands

```powershell
docker compose up -d
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

## Seeded Accounts

- Tenant: `default`
- Admin: `admin` / `admin123`
- Participant: `user` / `user123`

## Auth Endpoints

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/me`

## First Implementation Slice

1. Implement real auth with hashed users, JWT issue/refresh, and role checks.
2. Add workflow definition CRUD for admins.
3. Add activity start/timeline APIs for participants.
4. Add evidence upload API using `FileStorageService`.
5. Append audit events for every state-changing action.
