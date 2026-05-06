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
- `storage`: shared validation with local and MinIO adapters.
- `audit`: append-only compliance events.
- `reporting`: PDF/Excel export foundation.
- `notification`: notification delivery foundation.
- `common`: API envelope, errors, logging, request tracing.

## Local Database

Default connection:

```text
jdbc:postgresql://localhost:55432/activity_platform
username: activity_app
password: activity_app
```

Override with:

```powershell
$env:APP_DB_URL="jdbc:postgresql://localhost:55432/activity_platform"
$env:APP_DB_USERNAME="activity_app"
$env:APP_DB_PASSWORD="activity_app"
```

## Commands

```powershell
docker compose up -d
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

## Local Seed Users

Seed users are disabled by default in `application.yml`.

For local development, run with the `local` profile or set environment variables from `application-local.yml`, such as:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
$env:APP_SEED_ADMIN_USERNAME="<admin-username>"
$env:APP_SEED_ADMIN_PASSWORD="<admin-password>"
$env:APP_SEED_PARTICIPANT_USERNAME="<participant-username>"
$env:APP_SEED_PARTICIPANT_PASSWORD="<participant-password>"
```

Do not display default credentials in frontend screens.

## Auth Endpoints

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/me`
- `GET /api/v1/users`
- `POST /api/v1/users`
- `GET /api/v1/users/me`
- `GET /api/v1/users/{userId}`
- `PUT /api/v1/users/{userId}`
- `PATCH /api/v1/users/{userId}/status`
- `GET /api/v1/workflows`
- `POST /api/v1/workflows`
- `PUT /api/v1/workflows/{workflowId}`
- `PATCH /api/v1/workflows/{workflowId}/status`
- `GET /api/v1/activities`
- `POST /api/v1/activities`
- `PATCH /api/v1/activities/{activityId}/tasks/{activityTaskId}/status`
- `GET /api/v1/evidence`
- `POST /api/v1/evidence`
- `PATCH /api/v1/evidence/{evidenceId}/review`
- `GET /api/v1/reports/summary`
- `POST /api/v1/reports/export`
- `GET /api/v1/roles`
- `PUT /api/v1/users/{userId}/roles`
- `POST /api/v1/notifications`
- `GET /api/v1/notifications`
- `PATCH /api/v1/notifications/{notificationId}/status`

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

Expo web is allowed through CORS for `http://localhost:19006` by default. Override with `APP_CORS_ALLOWED_ORIGIN_WEB` if your frontend runs on another port.

## Storage

Local development uses disk storage by default:

```powershell
$env:APP_STORAGE_PROVIDER="local"
$env:APP_LOCAL_STORAGE_PATH="./storage/local"
```

Production should use MinIO or another S3-compatible endpoint behind the MinIO
adapter:

```powershell
$env:APP_STORAGE_PROVIDER="minio"
$env:APP_MINIO_ENDPOINT="https://minio.example.com"
$env:APP_MINIO_BUCKET="activity-platform"
$env:APP_MINIO_ACCESS_KEY="<access-key>"
$env:APP_MINIO_SECRET_KEY="<secret-key>"
$env:APP_MINIO_REGION="us-east-1"
$env:APP_MINIO_SECURE="true"
```

Both providers store objects with the same tenant/owner/object key structure.

The `prod` profile validates production safety at startup: external PostgreSQL,
strong JWT secret, HTTPS CORS origins, and MinIO storage are required.

## Verification

```powershell
.\mvnw.cmd test
.\mvnw.cmd -Pintegration-test verify
```

The integration profile uses Testcontainers PostgreSQL and exercises the API
contracts used by the frontend workflow, activity, evidence, reporting, role,
and notification flows.
