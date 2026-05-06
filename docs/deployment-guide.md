# Deployment Guide

This guide covers the current end-to-end deployment shape for the activity
compliance platform.

## Local Docker Stack

Run the full local stack from the repository root:

```powershell
docker compose up --build
```

Services:

- Frontend Expo web: `http://localhost:19006`
- Backend API: `http://localhost:8080`
- PostgreSQL host port: `localhost:55432`
- MinIO API: `http://localhost:9000`
- MinIO console: `http://localhost:9001`

The compose stack starts PostgreSQL, MinIO, the Spring Boot backend, and the
Expo web frontend. It uses the local Spring profile with MinIO enabled so proof
uploads exercise the same storage adapter shape expected in production.

Copy `.env.example` if you want to override local defaults before running
Compose. The frontend container reads `EXPO_PUBLIC_API_BASE_URL` and related
public Expo variables from Compose/environment instead of hardcoding the backend
origin.

## Production Runtime

Recommended production topology:

- HTTPS reverse proxy or load balancer in front of backend and frontend.
- Spring Boot backend running with `SPRING_PROFILES_ACTIVE=prod`.
- Managed PostgreSQL or hardened PostgreSQL VM/container.
- MinIO or S3-compatible object storage.
- External secret manager for database, JWT, and storage credentials.
- Centralized log collection for request IDs and audit investigation.

The `prod` profile fails startup if unsafe defaults are used for database, JWT,
CORS, or storage settings.

## Required Production Environment

```bash
SPRING_PROFILES_ACTIVE=prod
APP_DB_URL=jdbc:postgresql://db.example.com:5432/activity_platform
APP_DB_USERNAME=activity_app
APP_DB_PASSWORD=<strong-password>
APP_JWT_SECRET=<at-least-48-characters-random-secret>
APP_CORS_ALLOWED_ORIGINS=https://app.example.com
APP_STORAGE_PROVIDER=minio
APP_MINIO_ENDPOINT=https://minio.example.com
APP_MINIO_BUCKET=activity-platform
APP_MINIO_ACCESS_KEY=<access-key>
APP_MINIO_SECRET_KEY=<secret-key>
APP_MINIO_SECURE=true
APP_SEED_ENABLED=false
APP_LOG_LEVEL_ROOT=WARN
APP_LOG_LEVEL_APP=INFO
APP_LOG_LEVEL_SECURITY=WARN
APP_OPENAPI_ENABLED=false
APP_SWAGGER_UI_ENABLED=false
EXPO_PUBLIC_API_BASE_URL=https://api.example.com
EXPO_PUBLIC_API_VERSION=v1
```

Use `.env.production.example` as the complete environment template. Keep real
database, JWT, and MinIO credentials in the deployment platform or a secret
manager, not in source control.

## CI And Security Gates

The GitHub workflows currently provide:

- Frontend `npm ci`, `npm run typecheck`, and `npm run lint`.
- Backend unit tests with `./mvnw -B test`.
- Backend integration tests with Testcontainers through
  `./mvnw -B -Pintegration-test verify`.
- Docker Compose syntax/config validation.
- Frontend `npm audit --audit-level=high`.
- Backend OWASP Dependency Check with HTML/JSON report artifacts.

Both CI workflows support manual `workflow_dispatch` runs for release checks.

## Deployment Checklist

- Backend unit tests pass.
- Backend integration tests pass against Testcontainers PostgreSQL.
- Frontend typecheck and lint pass.
- CI workflow is enabled for pull requests.
- Security scan workflow is enabled, reviewed, and artifacts are checked.
- Production profile starts with real PostgreSQL and MinIO configuration.
- JWT secret is generated outside the repo and stored in a secret manager.
- CORS origins are explicit HTTPS origins.
- Frontend `EXPO_PUBLIC_API_BASE_URL` points at the production API origin.
- Database backup and restore have been tested.
- MinIO bucket backup/lifecycle policy is configured.
- Audit log retention is documented for the client.
- First admin/supervisor account is created through seed or controlled admin API.
- Client workflow definitions are created before participant onboarding.
- Report export storage location is verified.
- Monitoring/alerting covers backend health, database, MinIO, and error rate.

## Release Verification

Before handoff, run:

```powershell
npm run typecheck
npm run lint
cd backend
.\mvnw.cmd test
.\mvnw.cmd -Pintegration-test verify
.\mvnw.cmd -Psecurity-scan -DskipTests verify
```

Then smoke test:

- Admin login.
- Create participant.
- Create workflow definition.
- Assign/start participant activity.
- Participant proof upload.
- Admin evidence review.
- Report summary, PDF export, and Excel export.
